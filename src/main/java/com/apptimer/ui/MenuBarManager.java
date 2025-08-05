package com.apptimer.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apptimer.SecurityManager;
import com.apptimer.VoiceNotifier;
import com.apptimer.ProcessMonitor;
import com.apptimer.controller.MainApplicationController;

/**
 * Manages the application menu bar and menu actions
 */
public class MenuBarManager {
    private static final Logger logger = LoggerFactory.getLogger(MenuBarManager.class);
    
    private final Stage primaryStage;
    private final MainApplicationController controller;
    private final TextArea logArea;
    
    public MenuBarManager(Stage primaryStage, MainApplicationController controller, TextArea logArea) {
        this.primaryStage = primaryStage;
        this.controller = controller;
        this.logArea = logArea;
    }
    
    public MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // Security menu
        Menu securityMenu = createSecurityMenu();
        
        // Tools menu
        Menu toolsMenu = createToolsMenu();
        
        // Help menu
        Menu helpMenu = createHelpMenu();
        
        menuBar.getMenus().addAll(securityMenu, toolsMenu, helpMenu);
        return menuBar;
    }
    
    private Menu createSecurityMenu() {
        Menu securityMenu = new Menu("🔒 Security");
        
        MenuItem changePasswordItem = new MenuItem("🔑 Change Password...");
        changePasswordItem.setOnAction(e -> controller.getSecurityManager().changePassword(primaryStage));
        
        MenuItem logoutItem = new MenuItem("🚪 Logout");
        logoutItem.setOnAction(e -> {
            controller.getSecurityManager().logout();
            DialogManager.showInfoDialog("Logged Out", "You have been logged out. Authentication will be required for protected actions.");
        });
        
        MenuItem securityStatusItem = new MenuItem("📊 Security Status");
        securityStatusItem.setOnAction(e -> showSecurityStatusDialog());
        
        MenuItem testAuthItem = new MenuItem("🧪 Test Authentication");
        testAuthItem.setOnAction(e -> {
            if (controller.getSecurityManager().forceAuthenticate(primaryStage, "test authentication")) {
                DialogManager.showInfoDialog("Success", "Authentication successful!");
            }
        });
        
        securityMenu.getItems().addAll(changePasswordItem, logoutItem, securityStatusItem, testAuthItem);
        return securityMenu;
    }
    
    private Menu createToolsMenu() {
        Menu toolsMenu = new Menu("🛠️ Tools");
        
        MenuItem unblockAllItem = new MenuItem("🔓 Unblock All Applications");
        unblockAllItem.setOnAction(e -> handleUnblockAll());
        
        MenuItem testVoiceItem = new MenuItem("🔊 Test Voice Notifications");
        testVoiceItem.setOnAction(e -> controller.getVoiceNotifier().testVoice());
        
        MenuItem refreshStatusItem = new MenuItem("🔄 Refresh Status");
        refreshStatusItem.setOnAction(e -> {
            logArea.appendText("🔄 Status refreshed manually\n");
            // This would trigger the controller to update status labels
        });
        
        toolsMenu.getItems().addAll(unblockAllItem, testVoiceItem, refreshStatusItem);
        return toolsMenu;
    }
    
    private Menu createHelpMenu() {
        Menu helpMenu = new Menu("❓ Help");
        
        MenuItem aboutItem = new MenuItem("ℹ️ About");
        aboutItem.setOnAction(e -> showEnhancedAboutDialog());
        
        MenuItem securityHelpItem = new MenuItem("🛡️ Security Information");
        securityHelpItem.setOnAction(e -> showEnhancedSecurityHelpDialog());
        
        MenuItem featuresItem = new MenuItem("✨ Enhanced Features");
        featuresItem.setOnAction(e -> showFeaturesDialog());
        
        helpMenu.getItems().addAll(aboutItem, securityHelpItem, featuresItem);
        return helpMenu;
    }
    
    private void handleUnblockAll() {
        if (controller.getSecurityManager().authenticate(primaryStage, "unblock all applications")) {
            if (controller.getProcessMonitor().getApplicationBlocker() != null) {
                controller.getProcessMonitor().getApplicationBlocker().unblockApplication("minecraft.exe");
                controller.getProcessMonitor().getApplicationBlocker().unblockApplication("chrome.exe");
                logArea.appendText("🔓 All applications unblocked by administrator\n");
                logger.info("All applications unblocked by admin");
            }
        }
    }
    
    private void showSecurityStatusDialog() {
        String status = controller.getSecurityManager().getSecurityStatus();
        DialogManager.createCustomDialog(
            "🔒 Security Status",
            "Enhanced Security System Status",
            status
        ).showAndWait();
    }
    
    private void showEnhancedAboutDialog() {
        String content = 
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
            "🔧 Enhanced by Professional Java Developer";
        
        DialogManager.createCustomDialog(
            "ℹ️ About Enhanced App Time Limiter",
            "Enhanced App Time Limiter v2.0.0",
            content
        ).showAndWait();
    }
    
    private void showEnhancedSecurityHelpDialog() {
        String content = 
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
            "• Use 'Security → Change Password' menu";
        
        DialogManager.createCustomDialog(
            "🛡️ Enhanced Security Information",
            "Advanced Security Features",
            content
        ).showAndWait();
    }
    
    private void showFeaturesDialog() {
        String content = 
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
            "• Security preferences";
        
        DialogManager.createCustomDialog(
            "✨ Enhanced Features",
            "New in Enhanced Version 2.0",
            content
        ).showAndWait();
    }
}
