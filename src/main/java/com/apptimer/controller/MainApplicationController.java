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
    private TextArea logArea;
    
    // Background services
    private final ScheduledExecutorService protectionService;
    private volatile boolean isProtectionActive = false;
    private boolean wasAuthenticated = false; // Track previous authentication state
    
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
            
            // Restore blocked applications state
            Map<String, java.time.LocalDateTime> blockedApps = settingsManager.loadBlockedApplications();
            if (!blockedApps.isEmpty()) {
                logger.info("Restoring blocked applications state...");
            }
            
            // AUTO-START MONITORING with loaded settings
            startMonitoringWithSettings(settings, blockedApps);
            
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
            
            logger.info("Application initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize application", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }
    
    /**
     * Start monitoring with loaded settings
     */
    private void startMonitoringWithSettings(SettingsManager.AppSettings settings, Map<String, java.time.LocalDateTime> blockedApps) {
        timeTracker.setTimeLimit("minecraft.exe", settings.minecraftLimit * 60);
        timeTracker.setTimeLimit("chrome.exe", settings.chromeLimit * 60);
        timeTracker.setWarningTime(settings.warningTime * 60);
        
        processMonitor.startMonitoring(timeTracker, voiceNotifier);
        
        // Set block delays AFTER monitoring starts to ensure ApplicationBlocker exists
        if (processMonitor.getApplicationBlocker() != null) {
            processMonitor.getApplicationBlocker().setDefaultBlockDelay("minecraft.exe", settings.minecraftDelay);
            processMonitor.getApplicationBlocker().setDefaultBlockDelay("chrome.exe", settings.chromeDelay);
            
            // Set up callback to save state immediately when apps are blocked
            processMonitor.getApplicationBlocker().setSaveStateCallback(() -> saveCurrentState());
            
            // Restore blocked applications state
            if (blockedApps != null && !blockedApps.isEmpty()) {
                processMonitor.getApplicationBlocker().restoreBlockedState(blockedApps);
            }
        }
        
        // Check if any apps should be blocked based on restored time usage
        checkAndBlockExceededApps();
        
        logger.info("Enhanced monitoring started - Minecraft: {}min ({}min delay), Chrome: {}min ({}min delay), Warning: {}min", 
                   settings.minecraftLimit, settings.minecraftDelay, 
                   settings.chromeLimit, settings.chromeDelay, settings.warningTime);
    }
    
    /**
     * Start auto-refresh service for status updates
     */
    public void startAutoRefresh(Label minecraftStatusLabel, Label chromeStatusLabel) {
        // Update status immediately when starting auto-refresh (with small delay to ensure blocker is initialized)
        if (processMonitor != null && processMonitor.isMonitoring()) {
            Platform.runLater(() -> {
                try {
                    Thread.sleep(100); // Small delay to ensure ApplicationBlocker is fully initialized
                    updateStatusLabels(minecraftStatusLabel, chromeStatusLabel);
                    updateRemainingTimeDisplay();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Set up immediate status update callback for when processes start/stop
        if (processMonitor != null) {
            processMonitor.setStatusUpdateCallback(() -> {
                Platform.runLater(() -> {
                    updateStatusLabels(minecraftStatusLabel, chromeStatusLabel);
                });
            });
        }
        
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
        
        // Add automatic logout checker (checks every second)
        protectionService.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                boolean currentlyAuthenticated = securityManager.isAuthenticated();
                
                // Check for logout transition: was authenticated, now not authenticated
                if (wasAuthenticated && !currentlyAuthenticated) {
                    logArea.appendText("🔐 Session expired - automatically logged out after 3 minutes of inactivity\n");
                }
                
                // Update state for next check
                wasAuthenticated = currentlyAuthenticated;
            });
        }, 1, 1, TimeUnit.SECONDS);
        
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
            
            // Get blocked applications state
            Map<String, java.time.LocalDateTime> blockedState = null;
            if (processMonitor.getApplicationBlocker() != null) {
                blockedState = processMonitor.getApplicationBlocker().getBlockedUntilState();
            }
            
            settingsManager.saveSettings(settings, timeUsage, blockedState);
            
            logger.debug("Application state saved including blocked applications");
        } catch (Exception e) {
            logger.error("Error saving application state", e);
        }
    }
    
    /**
     * Update status labels with current time usage
     */
    public void updateStatusLabels(Label minecraftStatusLabel, Label chromeStatusLabel) {
        if (timeTracker != null && processMonitor != null) {
            SettingsManager.AppSettings settings = settingsManager.getCurrentSettings();
            
            // Update Minecraft status
            long minecraftUsed = timeTracker.getTotalTime("minecraft.exe") / 60;
            long minecraftLimit = settings.minecraftLimit;
            long minecraftRemaining = Math.max(0, minecraftLimit - minecraftUsed);
            
            // Check if Minecraft is blocked and running
            boolean minecraftBlocked = processMonitor.getApplicationBlocker().isBlocked("minecraft.exe");
            boolean minecraftRunning = processMonitor.isProcessRunning("minecraft.exe");
            
            // Get block delay info
            long minecraftBlockRemaining = 0;
            if (minecraftBlocked) {
                minecraftBlockRemaining = processMonitor.getApplicationBlocker().getRemainingBlockTime("minecraft.exe");
            }
            
            if (minecraftBlocked) {
                minecraftStatusLabel.setText(String.format("🔴 Minecraft: BLOCKED (%d min remaining)", minecraftBlockRemaining));
                minecraftStatusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;"); // Red text
            } else if (minecraftRunning) {
                minecraftStatusLabel.setText(String.format("🟠 Minecraft: RUNNING (%d min remaining)", minecraftRemaining));
                minecraftStatusLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;"); // Orange text for running
            } else {
                minecraftStatusLabel.setText(String.format("🟢 Minecraft: NOT RUNNING (%d min remaining)", minecraftRemaining));
                minecraftStatusLabel.setStyle("-fx-text-fill: #388e3c; -fx-font-weight: normal;"); // Green text
            }
            
            // Update Chrome status  
            long chromeUsed = timeTracker.getTotalTime("chrome.exe") / 60;
            long chromeLimit = settings.chromeLimit;
            long chromeRemaining = Math.max(0, chromeLimit - chromeUsed);
            
            // Check if Chrome is blocked and running
            boolean chromeBlocked = processMonitor.getApplicationBlocker().isBlocked("chrome.exe");
            boolean chromeRunning = processMonitor.isProcessRunning("chrome.exe");
            
            // Get block delay info
            long chromeBlockRemaining = 0;
            if (chromeBlocked) {
                chromeBlockRemaining = processMonitor.getApplicationBlocker().getRemainingBlockTime("chrome.exe");
            }
            
            if (chromeBlocked) {
                chromeStatusLabel.setText(String.format("🔴 Chrome: BLOCKED (%d min remaining)", chromeBlockRemaining));
                chromeStatusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;"); // Red text
            } else if (chromeRunning) {
                chromeStatusLabel.setText(String.format("🟠 Chrome: RUNNING (%d min remaining)", chromeRemaining));
                chromeStatusLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;"); // Orange text for running
            } else {
                chromeStatusLabel.setText(String.format("🟢 Chrome: NOT RUNNING (%d min remaining)", chromeRemaining));
                chromeStatusLabel.setStyle("-fx-text-fill: #388e3c; -fx-font-weight: normal;"); // Green text
            }
        }
    }
    
    /**
     * Update remaining time display in log
     */
    private void updateRemainingTimeDisplay() {
        if (timeTracker != null && processMonitor != null) {
            SettingsManager.AppSettings settings = settingsManager.getCurrentSettings();
            
            logArea.appendText("⏱️ Enhanced time status:\n");
            
            // Show Minecraft status
            long minecraftUsed = timeTracker.getTotalTime("minecraft.exe") / 60;
            long minecraftRemaining = Math.max(0, settings.minecraftLimit - minecraftUsed);
            
            // Check if Minecraft is blocked
            boolean minecraftBlocked = processMonitor.getApplicationBlocker().isBlocked("minecraft.exe");
            String minecraftIcon = minecraftBlocked ? "🔴" : "🟢";
            long minecraftBlockRemaining = minecraftBlocked ? 
                processMonitor.getApplicationBlocker().getRemainingBlockTime("minecraft.exe") : 0;
            
            if (minecraftBlocked) {
                logArea.appendText(String.format("   🔴 Minecraft: BLOCKED (%d minutes remaining)\n", minecraftBlockRemaining));
            } else {
                logArea.appendText(String.format("   🟢 Minecraft: %d/%d minutes used (%d minutes remaining)\n", 
                                 minecraftUsed, settings.minecraftLimit, minecraftRemaining));
            }
            
            // Show Chrome status  
            long chromeUsed = timeTracker.getTotalTime("chrome.exe") / 60;
            long chromeRemaining = Math.max(0, settings.chromeLimit - chromeUsed);
            
            // Check if Chrome is blocked
            boolean chromeBlocked = processMonitor.getApplicationBlocker().isBlocked("chrome.exe");
            String chromeIcon = chromeBlocked ? "🔴" : "🟢";
            long chromeBlockRemaining = chromeBlocked ? 
                processMonitor.getApplicationBlocker().getRemainingBlockTime("chrome.exe") : 0;
            
            if (chromeBlocked) {
                logArea.appendText(String.format("   🔴 Chrome: BLOCKED (%d minutes remaining)\n", chromeBlockRemaining));
            } else {
                logArea.appendText(String.format("   🟢 Chrome: %d/%d minutes used (%d minutes remaining)\n", 
                                 chromeUsed, settings.chromeLimit, chromeRemaining));
            }
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
    
    /**
     * Check if any applications should be blocked based on current time usage
     */
    private void checkAndBlockExceededApps() {
        if (timeTracker == null || processMonitor == null || processMonitor.getApplicationBlocker() == null) {
            return;
        }
        
        SettingsManager.AppSettings settings = settingsManager.getCurrentSettings();
        
        // Check Minecraft
        long minecraftUsedSeconds = timeTracker.getTotalTime("minecraft.exe");
        long minecraftLimitSeconds = settings.minecraftLimit * 60;
        if (minecraftUsedSeconds >= minecraftLimitSeconds) {
            logger.info("Minecraft usage ({} min) exceeds limit ({} min) - blocking immediately", 
                       minecraftUsedSeconds / 60, settings.minecraftLimit);
            processMonitor.getApplicationBlocker().blockMinecraftWithConfiguredDelay();
            timeTracker.resetTime("minecraft.exe"); // Prevent duplicate processing
        }
        
        // Check Chrome
        long chromeUsedSeconds = timeTracker.getTotalTime("chrome.exe");
        long chromeLimitSeconds = settings.chromeLimit * 60;
        if (chromeUsedSeconds >= chromeLimitSeconds) {
            logger.info("Chrome usage ({} min) exceeds limit ({} min) - blocking immediately", 
                       chromeUsedSeconds / 60, settings.chromeLimit);
            processMonitor.getApplicationBlocker().blockChromeWithConfiguredDelay();
            timeTracker.resetTime("chrome.exe"); // Prevent duplicate processing
        }
    }
    
    /**
     * Update the log area reference (needed when MainWindow is recreated)
     */
    public void updateLogArea(TextArea newLogArea) {
        this.logArea = newLogArea;
    }
    
    public boolean isProtectionActive() { return isProtectionActive; }
    public void setProtectionActive(boolean active) { this.isProtectionActive = active; }
}
