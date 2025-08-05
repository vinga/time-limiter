package com.apptimer.controller;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apptimer.*;
import com.apptimer.config.SettingsManager;
import com.apptimer.ui.SystemTrayManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * Main application controller that coordinates all components
 * and handles core application lifecycle
 */
public class MainApplicationController {
    private static final Logger logger = LoggerFactory.getLogger(MainApplicationController.class);
    
    // Core components
    private final ProcessMonitor processMonitor;
    private final TimeTracker timeTracker;
    private final VoiceNotifier voiceNotifier;
    private final com.apptimer.SecurityManager securityManager;
    private final SettingsManager settingsManager;
    private final SystemTrayManager trayManager;
    
    // UI components
    private final Stage primaryStage;
    private final TextArea logArea;
    
    // Background services
    private final ScheduledExecutorService protectionService;
    private volatile boolean isProtectionActive = false;
    
    public MainApplicationController(Stage primaryStage, TextArea logArea) {
        this.primaryStage = primaryStage;
        this.logArea = logArea;
        
        // Initialize core components
        this.processMonitor = new ProcessMonitor();
        this.timeTracker = new TimeTracker();
        this.voiceNotifier = new VoiceNotifier();
        this.securityManager = new com.apptimer.SecurityManager();
        this.settingsManager = new SettingsManager();
        this.trayManager = new SystemTrayManager(primaryStage, this);
        
        // Initialize background services
        this.protectionService = Executors.newScheduledThreadPool(1);
        
        logger.info("MainApplicationController initialized");
    }
    
    /**
     * Initialize the application with settings and start monitoring
     */
    public void initializeApplication() {
        try {
            // Load saved settings
            SettingsManager.AppSettings settings = settingsManager.loadSettings();
            
            // Apply password hash if exists
            if (!settings.passwordHash.isEmpty()) {
                securityManager.setPasswordHash(settings.passwordHash);
            }
            
            // Restore time usage data if from today
            Map<String, Long> timeUsage = settingsManager.loadTodaysTimeUsage();
            if (timeUsage != null) {
                timeUsage.forEach((processName, timeSeconds) -> {
                    timeTracker.setTotalTime(processName, timeSeconds);
                });
                logger.info("Time usage restored from today's session");
            } else {
                logger.info("Starting fresh time tracking (new day or no previous data)");
            }
            
            // AUTO-START MONITORING with loaded settings
            startMonitoringWithSettings(settings);
            
            // Start background services
            startProtectionService();
            startAutoSaveService();
            
            // Setup system tray
            trayManager.setupSystemTray();
            
            // Show initial status
            logArea.appendText(String.format("🟢 Enhanced monitoring auto-started: Minecraft=%dmin (delay=%dmin), Chrome=%dmin (delay=%dmin), Warning=%dmin\n", 
                              settings.minecraftLimit, settings.minecraftDelay, 
                              settings.chromeLimit, settings.chromeDelay, settings.warningTime));
            logArea.appendText("🔒 Enhanced app running in kid-safe mode - minimized to system tray\n");
            
            updateRemainingTimeDisplay();
            
            logger.info("Application initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize application", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }
    
    /**
     * Start monitoring with loaded settings
     */
    private void startMonitoringWithSettings(SettingsManager.AppSettings settings) {
        timeTracker.setTimeLimit("minecraft.exe", settings.minecraftLimit * 60);
        timeTracker.setTimeLimit("chrome.exe", settings.chromeLimit * 60);
        timeTracker.setWarningTime(settings.warningTime * 60);
        
        // Set block delays
        if (processMonitor.getApplicationBlocker() != null) {
            processMonitor.getApplicationBlocker().setDefaultBlockDelay("minecraft.exe", settings.minecraftDelay);
            processMonitor.getApplicationBlocker().setDefaultBlockDelay("chrome.exe", settings.chromeDelay);
        }
        
        processMonitor.startMonitoring(timeTracker, voiceNotifier);
        
        logger.info("Enhanced monitoring started - Minecraft: {}min ({}min delay), Chrome: {}min ({}min delay), Warning: {}min", 
                   settings.minecraftLimit, settings.minecraftDelay, 
                   settings.chromeLimit, settings.chromeDelay, settings.warningTime);
    }
    
    /**
     * Start auto-refresh service for status updates
     */
    public void startAutoRefresh(Label minecraftStatusLabel, Label chromeStatusLabel) {
        protectionService.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                if (processMonitor != null && processMonitor.isMonitoring()) {
                    updateStatusLabels(minecraftStatusLabel, chromeStatusLabel);
                    
                    // Occasionally update log too (every 5 minutes)
                    if (System.currentTimeMillis() % (5 * 60 * 1000) < 60000) {
                        logArea.appendText("\n🔄 === ENHANCED TIME STATUS UPDATE ===\n");
                        updateRemainingTimeDisplay();
                        updateBlockedApplicationsStatus();
                    }
                }
            });
        }, 60, 60, TimeUnit.SECONDS); // Every 60 seconds
        
        logger.info("Auto-refresh service started");
    }
    
    /**
     * Start protection service
     */
    private void startProtectionService() {
        protectionService.scheduleAtFixedRate(() -> {
            if (isProtectionActive) {
                logger.debug("Enhanced protection service active - monitoring: " + 
                           (processMonitor != null ? processMonitor.isMonitoring() : false));
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        isProtectionActive = true;
        logger.info("Enhanced protection service started");
    }
    
    /**
     * Start auto-save service
     */
    private void startAutoSaveService() {
        protectionService.scheduleAtFixedRate(() -> {
            saveCurrentState();
        }, 300, 300, TimeUnit.SECONDS); // Every 5 minutes
        
        logger.info("Auto-save service started");
    }
    
    /**
     * Save current application state
     */
    public void saveCurrentState() {
        try {
            SettingsManager.AppSettings settings = settingsManager.getCurrentSettings();
            settings.passwordHash = securityManager.getPasswordHash();
            
            Map<String, Long> timeUsage = timeTracker.getAllTotalTimes();
            settingsManager.saveSettings(settings, timeUsage);
            
            logger.debug("Application state saved");
        } catch (Exception e) {
            logger.error("Error saving application state", e);
        }
    }
    
    /**
     * Update status labels with current time usage
     */
    public void updateStatusLabels(Label minecraftStatusLabel, Label chromeStatusLabel) {
        if (timeTracker != null) {
            SettingsManager.AppSettings settings = settingsManager.getCurrentSettings();
            
            // Update Minecraft status
            long minecraftUsed = timeTracker.getTotalTime("minecraft.exe") / 60;
            long minecraftLimit = settings.minecraftLimit;
            long minecraftRemaining = Math.max(0, minecraftLimit - minecraftUsed);
            
            minecraftStatusLabel.setText(String.format("⏱️ Minecraft: %d/%d min (%d remaining)", 
                                        minecraftUsed, minecraftLimit, minecraftRemaining));
            
            // Update Chrome status
            long chromeUsed = timeTracker.getTotalTime("chrome.exe") / 60;
            long chromeLimit = settings.chromeLimit;
            long chromeRemaining = Math.max(0, chromeLimit - chromeUsed);
            
            chromeStatusLabel.setText(String.format("⏱️ Chrome: %d/%d min (%d remaining)", 
                                    chromeUsed, chromeLimit, chromeRemaining));
        }
    }
    
    /**
     * Update remaining time display in log
     */
    private void updateRemainingTimeDisplay() {
        if (timeTracker != null) {
            SettingsManager.AppSettings settings = settingsManager.getCurrentSettings();
            
            logArea.appendText("⏱️ Enhanced time status:\n");
            
            // Show Minecraft status
            long minecraftUsed = timeTracker.getTotalTime("minecraft.exe") / 60;
            long minecraftRemaining = Math.max(0, settings.minecraftLimit - minecraftUsed);
            
            logArea.appendText(String.format("   Minecraft: %d/%d minutes used (%d remaining)\n", 
                             minecraftUsed, settings.minecraftLimit, minecraftRemaining));
            
            // Show Chrome status  
            long chromeUsed = timeTracker.getTotalTime("chrome.exe") / 60;
            long chromeRemaining = Math.max(0, settings.chromeLimit - chromeUsed);
            
            logArea.appendText(String.format("   Chrome: %d/%d minutes used (%d remaining)\n", 
                             chromeUsed, settings.chromeLimit, chromeRemaining));
        }
    }
    
    /**
     * Update blocked applications status in log
     */
    private void updateBlockedApplicationsStatus() {
        if (processMonitor.getApplicationBlocker() != null) {
            String status = processMonitor.getApplicationBlocker().getBlockedApplicationsStatus();
            if (!status.equals("No applications currently blocked")) {
                logArea.appendText("📋 " + status);
            }
        }
    }
    
    /**
     * Shutdown application cleanly
     */
    public void shutdown() {
        logger.info("Application shutting down...");
        
        // Save current state
        saveCurrentState();
        
        // Stop monitoring
        if (processMonitor.isMonitoring()) {
            processMonitor.stopMonitoring();
            logger.info("Monitoring stopped");
        }
        
        // Shutdown services
        if (protectionService != null) {
            protectionService.shutdown();
            logger.info("Protection service shutdown");
        }
        
        logger.info("Application shutdown complete");
    }
    
    // Getters for components
    public ProcessMonitor getProcessMonitor() { return processMonitor; }
    public TimeTracker getTimeTracker() { return timeTracker; }
    public VoiceNotifier getVoiceNotifier() { return voiceNotifier; }
    public com.apptimer.SecurityManager getSecurityManager() { return securityManager; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public SystemTrayManager getTrayManager() { return trayManager; }
    
    public boolean isProtectionActive() { return isProtectionActive; }
    public void setProtectionActive(boolean active) { this.isProtectionActive = active; }
}
