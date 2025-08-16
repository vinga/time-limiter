package com.apptimer.ui;

import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apptimer.*;
import com.apptimer.config.SettingsManager;

/**
 * Handles UI events and user interactions
 */
public class EventHandlers {
    private static final Logger logger = LoggerFactory.getLogger(EventHandlers.class);
    
    private final Stage primaryStage;
    private final ProcessMonitor processMonitor;
    private final TimeTracker timeTracker;
    private final VoiceNotifier voiceNotifier;
    private final com.apptimer.SecurityManager securityManager;
    private final SettingsManager settingsManager;
    private final SystemTrayManager trayManager;
    private final TextArea logArea;
    
    // UI field references
    private final TextField minecraftTimeField;
    private final TextField chromeTimeField;
    private final TextField warningTimeField;
    private final TextField minecraftDelayField;
    private final TextField chromeDelayField;
    
    public EventHandlers(Stage primaryStage, ProcessMonitor processMonitor, TimeTracker timeTracker,
                        VoiceNotifier voiceNotifier, com.apptimer.SecurityManager securityManager, 
                        SettingsManager settingsManager, SystemTrayManager trayManager,
                        TextArea logArea, TextField minecraftTimeField, TextField chromeTimeField,
                        TextField warningTimeField, TextField minecraftDelayField, TextField chromeDelayField) {
        this.primaryStage = primaryStage;
        this.processMonitor = processMonitor;
        this.timeTracker = timeTracker;
        this.voiceNotifier = voiceNotifier;
        this.securityManager = securityManager;
        this.settingsManager = settingsManager;
        this.trayManager = trayManager;
        this.logArea = logArea;
        this.minecraftTimeField = minecraftTimeField;
        this.chromeTimeField = chromeTimeField;
        this.warningTimeField = warningTimeField;
        this.minecraftDelayField = minecraftDelayField;
        this.chromeDelayField = chromeDelayField;
    }
    
    /**
     * Handle update button click for settings modification
     */
    public void handleEditButtonClick() {
        if (securityManager.authenticate(primaryStage, "update application settings and block delays")) {
            // Apply changes immediately (fields are always editable)
            try {
                int minecraftLimit = Integer.parseInt(minecraftTimeField.getText());
                int chromeLimit = Integer.parseInt(chromeTimeField.getText());
                int warningTime = Integer.parseInt(warningTimeField.getText());
                int minecraftDelay = Integer.parseInt(minecraftDelayField.getText());
                int chromeDelay = Integer.parseInt(chromeDelayField.getText());
                
                // Validate inputs
                validateSettings(minecraftLimit, chromeLimit, warningTime, minecraftDelay, chromeDelay);
                
                // Apply changes to active monitoring immediately
                timeTracker.setTimeLimit("minecraft.exe", minecraftLimit * 60);
                timeTracker.setTimeLimit("chrome.exe", chromeLimit * 60);
                timeTracker.setWarningTime(warningTime * 60);
                
                // Update block delays
                if (processMonitor.getApplicationBlocker() != null) {
                    processMonitor.getApplicationBlocker().setDefaultBlockDelay("minecraft.exe", minecraftDelay);
                    processMonitor.getApplicationBlocker().setDefaultBlockDelay("chrome.exe", chromeDelay);
                }
                
                // Update stored settings and save to file
                SettingsManager.AppSettings settings = settingsManager.getCurrentSettings();
                settings.minecraftLimit = minecraftLimit;
                settings.chromeLimit = chromeLimit;
                settings.warningTime = warningTime;
                settings.minecraftDelay = minecraftDelay;
                settings.chromeDelay = chromeDelay;
                settings.passwordHash = securityManager.getPasswordHash();
                
                settingsManager.updateSettings(settings);
                settingsManager.saveSettings(settings, timeTracker.getAllTotalTimes());
                
                logArea.appendText("💾 Settings updated and applied to active monitoring\n");
                logger.info("Settings updated: Minecraft={}min ({}min delay), Chrome={}min ({}min delay), Warning={}min", 
                          minecraftLimit, minecraftDelay, chromeLimit, chromeDelay, warningTime);
                
            } catch (NumberFormatException ex) {
                DialogManager.showErrorDialog("Please enter valid numbers for all settings");
            } catch (IllegalArgumentException ex) {
                DialogManager.showErrorDialog(ex.getMessage());
            }
        }
    }
    
    private void validateSettings(int minecraftLimit, int chromeLimit, int warningTime, 
                                 int minecraftDelay, int chromeDelay) {
        if (minecraftLimit < 1 || minecraftLimit > 600) {
            throw new IllegalArgumentException("Minecraft limit must be between 1 and 600 minutes");
        }
        if (chromeLimit < 1 || chromeLimit > 600) {
            throw new IllegalArgumentException("Chrome limit must be between 1 and 600 minutes");
        }
        if (warningTime < 1 || warningTime > 30) {
            throw new IllegalArgumentException("Warning time must be between 1 and 30 minutes");
        }
        if (minecraftDelay < 5 || minecraftDelay > 1440) {
            throw new IllegalArgumentException("Block delays must be between 5 and 1440 minutes (24 hours)");
        }
        if (chromeDelay < 5 || chromeDelay > 1440) {
            throw new IllegalArgumentException("Block delays must be between 5 and 1440 minutes (24 hours)");
        }
    }
    
    /**
     * Handle manual application blocking
     */
    public void handleManualBlock(String processName, TextField minutesField) {
        if (securityManager.authenticate(primaryStage, "manually block " + getAppDisplayName(processName))) {
            try {
                int minutes = Integer.parseInt(minutesField.getText());
                if (minutes <= 0 || minutes > 1440) {
                    DialogManager.showErrorDialog("Please enter a valid number of minutes (1-1440)");
                    return;
                }
                
                if (processMonitor.getApplicationBlocker() != null) {
                    processMonitor.getApplicationBlocker().blockApplicationMinutes(processName, minutes, "Manually blocked by administrator");
                    
                    String appName = getAppDisplayName(processName);
                    logBlockingAction(appName, minutes);
                    
                    long blockRemaining = processMonitor.getApplicationBlocker().getRemainingBlockTime(processName);
                    logArea.appendText(String.format("⏰ %d minutes remaining in block\n", blockRemaining));
                } else {
                    DialogManager.showErrorDialog("Application blocker not initialized. Please ensure monitoring is active.");
                }
            } catch (NumberFormatException ex) {
                DialogManager.showErrorDialog("Please enter a valid number of minutes");
            }
        }
    }
    
    private void logBlockingAction(String appName, int minutes) {
        if (minutes < 60) {
            logArea.appendText(String.format("🚫 %s manually blocked for %d minutes\n", appName, minutes));
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                logArea.appendText(String.format("🚫 %s manually blocked for %d hours\n", appName, hours));
            } else {
                logArea.appendText(String.format("🚫 %s manually blocked for %dh %dm\n", appName, hours, remainingMinutes));
            }
        }
    }
    
    /**
     * Handle resume monitoring
     */
    public void handleStartMonitoring() {
        try {
            int minecraftLimit = Integer.parseInt(minecraftTimeField.getText());
            int chromeLimit = Integer.parseInt(chromeTimeField.getText());
            int warningTime = Integer.parseInt(warningTimeField.getText());
            
            timeTracker.setTimeLimit("minecraft.exe", minecraftLimit * 60);
            timeTracker.setTimeLimit("chrome.exe", chromeLimit * 60);
            timeTracker.setWarningTime(warningTime * 60);
            
            processMonitor.startMonitoring(timeTracker, voiceNotifier);
            voiceNotifier.announceMonitoringResumed();
            
            logArea.appendText("▶️ Monitoring resumed with current settings\n");
            
            // Show current status immediately
            showCurrentStatus();
            
            logger.info("Monitoring resumed - Minecraft: {}min, Chrome: {}min", minecraftLimit, chromeLimit);
            
        } catch (NumberFormatException ex) {
            DialogManager.showErrorDialog("Please enter valid numbers for time limits");
        }
    }
    
    /**
     * Handle pause monitoring
     */
    public void handleStopMonitoring() {
        if (securityManager.authenticate(primaryStage, "pause application monitoring")) {
            processMonitor.stopMonitoring();
            voiceNotifier.announceMonitoringStopped();
            
            logArea.appendText("⏸️ Monitoring paused by administrator\n");
            logger.info("Monitoring paused by admin authentication");
        }
    }
    
    /**
     * Handle application exit
     */
    public void handleExitApplication() {
        if (securityManager.authenticate(primaryStage, "exit the protected application")) {
            // Stop monitoring first
            if (processMonitor.isMonitoring()) {
                processMonitor.stopMonitoring();
                logger.info("Monitoring stopped before application exit");
            }
            
            // Save settings before exit
            SettingsManager.AppSettings settings = settingsManager.getCurrentSettings();
            settings.passwordHash = securityManager.getPasswordHash();
            settingsManager.saveSettings(settings, timeTracker.getAllTotalTimes());
            
            System.exit(0);
        }
    }
    
    /**
     * Handle unblock all applications
     */
    public void handleUnblockAll() {
        if (securityManager.authenticate(primaryStage, "unblock all applications")) {
            if (processMonitor.getApplicationBlocker() != null) {
                processMonitor.getApplicationBlocker().unblockApplication("minecraft.exe");
                processMonitor.getApplicationBlocker().unblockApplication("chrome.exe");
                logArea.appendText("🔓 All applications unblocked by administrator\n");
                logger.info("All applications unblocked by admin");
            }
        }
    }
    
    /**
     * Handle status refresh
     */
    public void handleRefreshStatus() {
        // This would trigger status updates in the main application
        logArea.appendText("🔄 Status refreshed manually\n");
    }
    
    /**
     * Show current time status in log
     */
    private void showCurrentStatus() {
        try {
            int minecraftLimit = Integer.parseInt(minecraftTimeField.getText());
            int chromeLimit = Integer.parseInt(chromeTimeField.getText());
            
            logArea.appendText("⏱️ Current time status:\n");
            
            // Show Minecraft status
            long minecraftUsed = timeTracker.getTotalTime("minecraft.exe") / 60;
            long minecraftRemaining = Math.max(0, minecraftLimit - minecraftUsed);
            
            logArea.appendText(String.format("   Minecraft: %d/%d minutes used (%d remaining)\n", 
                             minecraftUsed, minecraftLimit, minecraftRemaining));
            
            // Show Chrome status  
            long chromeUsed = timeTracker.getTotalTime("chrome.exe") / 60;
            long chromeRemaining = Math.max(0, chromeLimit - chromeUsed);
            
            logArea.appendText(String.format("   Chrome: %d/%d minutes used (%d remaining)\n", 
                             chromeUsed, chromeLimit, chromeRemaining));
        } catch (Exception e) {
            logArea.appendText("   Status: Unable to display current usage\n");
        }
    }
    
    private String getAppDisplayName(String processName) {
        switch (processName.toLowerCase()) {
            case "minecraft.exe":
                return "Minecraft";
            case "chrome.exe":
                return "Chrome";
            default:
                return processName;
        }
    }
}
