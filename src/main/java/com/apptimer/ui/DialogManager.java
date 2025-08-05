package com.apptimer.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import com.apptimer.ProcessMonitor;
import com.apptimer.TimeTracker;
import com.apptimer.SecurityManager;

/**
 * Manages various application dialogs and alerts
 */
public class DialogManager {
    
    /**
     * Show status dialog from system tray
     */
    public static void showStatusDialog(ProcessMonitor processMonitor, TimeTracker timeTracker, 
                                       SecurityManager securityManager, int minecraftLimit, int chromeLimit) {
        Alert statusAlert = new Alert(Alert.AlertType.INFORMATION);
        statusAlert.setTitle("📊 Enhanced App Time Limiter Status");
        statusAlert.setHeaderText("Current System Status");
        
        StringBuilder status = new StringBuilder();
        status.append("🔒 Enhanced Security Mode: ACTIVE\n\n");
        
        // Monitoring status
        if (processMonitor != null && processMonitor.isMonitoring()) {
            status.append("📊 Monitoring: ACTIVE\n");
            
            // Time usage
            if (timeTracker != null) {
                long minecraftUsed = timeTracker.getTotalTime("minecraft.exe") / 60;
                long chromeUsed = timeTracker.getTotalTime("chrome.exe") / 60;
                status.append(String.format("⏱️ Minecraft: %d/%d minutes used\n", minecraftUsed, minecraftLimit));
                status.append(String.format("⏱️ Chrome: %d/%d minutes used\n", chromeUsed, chromeLimit));
            }
        } else {
            status.append("📊 Monitoring: STOPPED\n");
        }
        
        // Blocked applications
        if (processMonitor != null && processMonitor.getApplicationBlocker() != null) {
            String blockedStatus = processMonitor.getApplicationBlocker().getBlockedApplicationsStatus();
            status.append("\n🚫 ").append(blockedStatus).append("\n");
        }
        
        // Security status
        if (securityManager != null) {
            status.append("\n🔒 Security Status:\n");
            status.append(securityManager.getSecurityStatus());
        }
        
        statusAlert.setContentText(status.toString());
        statusAlert.showAndWait();
    }
    
    /**
     * Show exit confirmation dialog from tray
     */
    public static boolean showTrayExitDialog() {
        Alert exitWarning = new Alert(Alert.AlertType.CONFIRMATION);
        exitWarning.setTitle("🔒 Exit Protected Application");
        exitWarning.setHeaderText("⚠️ Confirm Application Exit");
        exitWarning.setContentText("Are you sure you want to exit the Enhanced App Time Limiter?\n\n" +
                                  "This will stop all monitoring and close the application completely.\n" +
                                  "Administrator authentication is required.");
        exitWarning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        
        return exitWarning.showAndWait()
                .map(response -> response == ButtonType.YES)
                .orElse(false);
    }
    
    /**
     * Show enhanced about dialog
     */
    public static void showEnhancedAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ℹ️ About Enhanced App Time Limiter");
        alert.setHeaderText("Enhanced App Time Limiter v2.0.0");
        alert.setContentText(
            "🔒 Advanced Application Time Management System\n\n" +
            "🆕 Enhanced Features:\n" +
            "• Configurable block delays after time limits\n" +
            "• Advanced security with lockout protection\n" +
            "• Enhanced UI with organized sections\n" +
            "• Comprehensive voice notifications\n" +
            "• Real-time status monitoring\n" +
            "• Persistent settings with daily reset\n" +
            "• System tray integration\n" +
            "• Aggressive process termination\n\n" +
            "🎯 Monitored Applications: Minecraft, Chrome\n" +
            "🛠️ Built with JavaFX & Gradle\n" +
            "🔧 Enhanced by Professional Java Developer"
        );
        alert.showAndWait();
    }
    
    /**
     * Show enhanced security help dialog
     */
    public static void showEnhancedSecurityHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🛡️ Enhanced Security Information");
        alert.setHeaderText("Advanced Security Features");
        alert.setContentText(
            "🔒 ENHANCED SECURITY FEATURES:\n\n" +
            "• Default password: admin123 (CHANGE IMMEDIATELY!)\n" +
            "• Failed attempt tracking (5 max before lockout)\n" +
            "• 5-minute security lockout after max attempts\n" +
            "• Voice alerts for security violations\n" +
            "• Enhanced keyboard shortcut blocking\n" +
            "• Forced window closure protection\n" +
            "• Authentication timeout: 30 minutes\n\n" +
            "🛡️ CONFIGURABLE BLOCK DELAYS:\n" +
            "• Minecraft default: 60 minutes after limit\n" +
            "• Chrome default: 60 minutes after limit\n" +
            "• Range: 5 minutes to 24 hours\n" +
            "• Configurable per application\n\n" +
            "⚡ ENHANCED MONITORING:\n" +
            "• 2-second process checking\n" +
            "• Aggressive process termination\n" +
            "• Automatic restart protection\n" +
            "• Real-time status updates\n\n" +
            "⚠️ IMPORTANT:\n" +
            "• Change default password immediately!\n" +
            "• All security events are logged\n" +
            "• Use 'Security → Change Password' menu"
        );
        alert.showAndWait();
    }
    
    /**
     * Show features dialog
     */
    public static void showFeaturesDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("✨ Enhanced Features");
        alert.setHeaderText("New in Enhanced Version 2.0");
        alert.setContentText(
            "🆕 NEW FEATURES:\n\n" +
            "📊 ENHANCED UI ORGANIZATION:\n" +
            "• Organized sections with color coding\n" +
            "• Real-time status indicators\n" +
            "• Separate control and monitoring panels\n" +
            "• Comprehensive activity logging\n\n" +
            "⏳ CONFIGURABLE BLOCK DELAYS:\n" +
            "• Set custom block duration per app\n" +
            "• Automatic blocking after time limits\n" +
            "• Manual blocking with admin auth\n" +
            "• Block time remaining indicators\n\n" +
            "🔒 ADVANCED SECURITY:\n" +
            "• Failed attempt counting\n" +
            "• Automatic security lockouts\n" +
            "• Voice security alerts\n" +
            "• Enhanced keyboard protection\n\n" +
            "🔊 ENHANCED VOICE NOTIFICATIONS:\n" +
            "• Block expiration announcements\n" +
            "• Security violation alerts\n" +
            "• Monitoring start/stop notifications\n" +
            "• Manual unblock announcements\n\n" +
            "💾 PERSISTENT SETTINGS:\n" +
            "• All settings saved automatically\n" +
            "• Daily time usage tracking\n" +
            "• Block delay configurations\n" +
            "• Security preferences"
        );
        alert.showAndWait();
    }
    
    /**
     * Show security status dialog
     */
    public static void showSecurityStatusDialog(SecurityManager securityManager) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🔒 Security Status");
        alert.setHeaderText("Enhanced Security System Status");
        
        if (securityManager != null) {
            alert.setContentText(securityManager.getSecurityStatus());
        } else {
            alert.setContentText("Security manager not initialized");
        }
        
        alert.showAndWait();
    }
    
    /**
     * Show generic info dialog
     */
    public static void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Create custom dialog
     */
    public static Alert createCustomDialog(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        return alert;
    }
    
    /**
     * Show error dialog
     */
    public static void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
