package com.apptimer.ui;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;

import com.apptimer.ProcessMonitor;
import com.apptimer.TimeTracker;
import com.apptimer.SecurityManager;
import com.apptimer.controller.MainApplicationController;

/**
 * Manages system tray integration and functionality
 */
public class SystemTrayManager {
    private static final Logger logger = LoggerFactory.getLogger(SystemTrayManager.class);
    
    private TrayIcon trayIcon;
    private final Stage primaryStage;
    private final MainApplicationController controller;
    
    public SystemTrayManager(Stage primaryStage, MainApplicationController controller) {
        this.primaryStage = primaryStage;
        this.controller = controller;
    }
    
    /**
     * Setup system tray integration
     */
    public void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            logger.warn("System tray not supported");
            return;
        }
        
        try {
            SystemTray systemTray = SystemTray.getSystemTray();
            
            // Create tray icon
            java.awt.Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
            
            PopupMenu popup = createEnhancedTrayPopupMenu();
            
            trayIcon = new TrayIcon(image, "🔒 Enhanced App Time Limiter - Security Mode Active", popup);
            trayIcon.setImageAutoSize(true);
            
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                logger.info("System tray icon double-clicked");
                toggleWindowVisibility();
            }));
            
            systemTray.add(trayIcon);
            logger.info("Enhanced system tray icon created");
            
        } catch (AWTException e) {
            logger.error("Failed to create system tray icon", e);
        }
    }
    
    private PopupMenu createEnhancedTrayPopupMenu() {
        PopupMenu popup = new PopupMenu();
        
        java.awt.MenuItem showItem = new java.awt.MenuItem("🔍 Show Control Panel");
        showItem.addActionListener(e -> Platform.runLater(() -> showWindow()));
        
        java.awt.MenuItem hideItem = new java.awt.MenuItem("📱 Hide to Tray");
        hideItem.addActionListener(e -> Platform.runLater(() -> hideWindow()));
        
        java.awt.MenuItem statusItem = new java.awt.MenuItem("📊 View Status");
        statusItem.addActionListener(e -> Platform.runLater(() -> showStatusDialog()));
        
        java.awt.MenuItem exitItem = new java.awt.MenuItem("🔒 Exit Application (Admin Required)");
        exitItem.addActionListener(e -> Platform.runLater(() -> handleTrayExit()));
        
        popup.add(showItem);
        popup.add(hideItem);
        popup.addSeparator();
        popup.add(statusItem);
        popup.addSeparator();
        popup.add(exitItem);
        
        return popup;
    }
    
    private void handleTrayExit() {
        if (controller.getSecurityManager().authenticate(primaryStage, "exit the application via system tray")) {
            controller.shutdown();
            Platform.exit();
            System.exit(0);
        }
    }
    
    private void showStatusDialog() {
        var settings = controller.getSettingsManager().getCurrentSettings();
        String statusText = String.format(
            "Enhanced App Time Limiter Status:\n\n" +
            "Monitoring: %s\n" +
            "Security: %s\n\n" +
            "Time Limits:\n" +
            "• Minecraft: %d minutes\n" +
            "• Chrome: %d minutes\n\n" +
            "Current Usage:\n" +
            "• Minecraft: %d minutes\n" +
            "• Chrome: %d minutes",
            controller.getProcessMonitor().isMonitoring() ? "ACTIVE" : "STOPPED",
            "PROTECTED",
            settings.minecraftLimit,
            settings.chromeLimit,
            controller.getTimeTracker().getTotalTime("minecraft.exe") / 60,
            controller.getTimeTracker().getTotalTime("chrome.exe") / 60
        );
        
        DialogManager.createCustomDialog(
            "📊 Application Status",
            "Enhanced App Time Limiter",
            statusText
        ).showAndWait();
    }
    
    public void showWindow() {
        if (primaryStage != null) {
            logger.info("Showing enhanced control panel from tray");
            
            try {
                if (primaryStage.isIconified()) {
                    primaryStage.setIconified(false);
                }
                
                primaryStage.toFront();
                primaryStage.requestFocus();
                
                logger.info("Enhanced control panel restored successfully");
                
            } catch (Exception e) {
                logger.error("Error showing enhanced control panel", e);
            }
        }
    }
    
    public void hideWindow() {
        if (primaryStage != null) {
            logger.info("Hiding enhanced control panel to tray");
            
            try {
                primaryStage.setIconified(true);
                logger.info("Enhanced control panel minimized successfully");
                
                if (trayIcon != null) {
                    trayIcon.displayMessage("🔒 Enhanced App Time Limiter", 
                        "Control panel minimized. Right-click tray icon to access.", 
                        TrayIcon.MessageType.INFO);
                }
                
            } catch (Exception e) {
                logger.error("Error hiding enhanced control panel", e);
            }
        }
    }
    
    public void toggleWindowVisibility() {
        if (primaryStage != null) {
            if (primaryStage.isShowing() && !primaryStage.isIconified()) {
                hideWindow();
            } else {
                showWindow();
            }
        }
    }
    
    public TrayIcon getTrayIcon() {
        return trayIcon;
    }
    
    public void displayMessage(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, messageType);
        }
    }
}
