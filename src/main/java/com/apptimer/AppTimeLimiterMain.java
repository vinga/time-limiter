package com.apptimer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;

/**
 * Main application class for App Time Limiter
 * Monitors and limits application usage time on Windows
 */
public class AppTimeLimiterMain extends Application {
    private static final Logger logger = LoggerFactory.getLogger(AppTimeLimiterMain.class);
    
    private ProcessMonitor processMonitor;
    private TimeTracker timeTracker;
    private VoiceNotifier voiceNotifier;
    private TrayIcon trayIcon;
    private SecurityManager securityManager;
    private Stage primaryStage;
    
    public static void main(String[] args) {
        logger.info("Starting App Time Limiter...");
        launch(args);
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // Security menu
        Menu securityMenu = new Menu("Security");
        
        MenuItem changePasswordItem = new MenuItem("Change Password...");
        changePasswordItem.setOnAction(e -> securityManager.changePassword(primaryStage));
        
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> {
            securityManager.logout();
            showInfoDialog("Logged Out", "You have been logged out. Authentication will be required for protected actions.");
        });
        
        MenuItem forceAuthItem = new MenuItem("Test Authentication");
        forceAuthItem.setOnAction(e -> {
            if (securityManager.forceAuthenticate(primaryStage, "test authentication")) {
                showInfoDialog("Success", "Authentication successful!");
            }
        });
        
        securityMenu.getItems().addAll(changePasswordItem, logoutItem, forceAuthItem);
        
        // Help menu
        Menu helpMenu = new Menu("Help");
        
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        
        MenuItem securityHelpItem = new MenuItem("Security Info");
        securityHelpItem.setOnAction(e -> showSecurityHelpDialog());
        
        helpMenu.getItems().addAll(aboutItem, securityHelpItem);
        
        menuBar.getMenus().addAll(securityMenu, helpMenu);
        return menuBar;
    }
    
    private void showSecurityHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Security Information");
        alert.setHeaderText("App Time Limiter Security Features");
        alert.setContentText(
            "🔒 SECURITY FEATURES:\n\n" +
            "• Default password: admin123\n" +
            "• Default time limits: 75 minutes each\n" +
            "• Password required for:\n" +
            "  - Modifying time limits\n" +
            "  - Stopping monitoring\n" +
            "  - Closing the application\n" +
            "  - System tray exit\n" +
            "• No password required for:\n" +
            "  - Starting monitoring\n" +
            "• Authentication timeout: 30 minutes\n" +
            "• All attempts are logged\n" +
            "• Application cannot be force-closed\n\n" +
            "⚠️ IMPORTANT:\n" +
            "Change the default password immediately!\n" +
            "Use Security → Change Password"
        );
        alert.showAndWait();
    }
    
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("App Time Limiter v1.0.0");
        alert.setContentText(
            "🔒 Secure Application Time Monitor\n\n" +
            "Features:\n" +
            "• Time-based application limiting\n" +
            "• Voice warnings before limits\n" +
            "• Password protection\n" +
            "• System tray integration\n" +
            "• Comprehensive logging\n\n" +
            "Monitors: Minecraft, Chrome\n" +
            "Built with JavaFX & Gradle"
        );
        alert.showAndWait();
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        try {
            initializeComponents();
            setupSystemTray();
            createMainWindow(primaryStage);
            
            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorDialog("Failed to start application: " + e.getMessage());
        }
    }
    
    private void initializeComponents() {
        processMonitor = new ProcessMonitor();
        timeTracker = new TimeTracker();
        voiceNotifier = new VoiceNotifier();
        securityManager = new SecurityManager();
        
        // Start monitoring in background
        processMonitor.startMonitoring(timeTracker, voiceNotifier);
    }
    
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            logger.warn("System tray not supported");
            return;
        }
        
        try {
            SystemTray systemTray = SystemTray.getSystemTray();
            
            // Create tray icon (you'll need to add an icon file)
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
            
            PopupMenu popup = new PopupMenu();
            
            java.awt.MenuItem showItem = new java.awt.MenuItem("Show Application");
            showItem.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.setIconified(false);
                primaryStage.toFront();
                primaryStage.requestFocus();
            }));
            
            java.awt.MenuItem exitItem = new java.awt.MenuItem("🔒 Exit Application (Admin Required)");
            exitItem.addActionListener(e -> {
                Platform.runLater(() -> {
                    // Show explicit warning before authentication
                    Alert exitWarning = new Alert(Alert.AlertType.CONFIRMATION);
                    exitWarning.setTitle("Exit Protected Application");
                    exitWarning.setHeaderText("⚠️ Confirm Application Exit");
                    exitWarning.setContentText("Are you sure you want to exit the App Time Limiter?\n\nThis will stop all monitoring and close the application completely.\nAdministrator authentication is required.");
                    exitWarning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                    
                    exitWarning.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            if (securityManager.authenticate(primaryStage, "exit the application via system tray")) {
                                if (processMonitor.isMonitoring()) {
                                    processMonitor.stopMonitoring();
                                    logger.info("Monitoring stopped before system tray exit");
                                }
                                Platform.exit();
                                System.exit(0);
                            }
                        }
                    });
                });
            });
            
            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);
            
            trayIcon = new TrayIcon(image, "🔒 App Time Limiter - Protected", popup);
            trayIcon.setImageAutoSize(true);
            
            // Add double-click handler to show application
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                if (primaryStage.isShowing() && !primaryStage.isIconified()) {
                    primaryStage.hide();
                } else {
                    primaryStage.show();
                    primaryStage.setIconified(false);
                    primaryStage.toFront();
                    primaryStage.requestFocus();
                }
            }));
            
            systemTray.add(trayIcon);
            logger.info("System tray icon created");
            
        } catch (AWTException e) {
            logger.error("Failed to create system tray icon", e);
        }
    }
    
    private void createMainWindow(Stage primaryStage) {
        primaryStage.setTitle("🔒 App Time Limiter - Protected");
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        
        // Create menu bar with security options
        MenuBar menuBar = createMenuBar();
        
        Label titleLabel = new Label("Application Time Limiter");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label securityLabel = new Label("🔐 Protected Mode - Admin access required for changes");
        securityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        
        // Time limit controls
        Label minecraftLabel = new Label("Minecraft time limit (minutes):");
        TextField minecraftTimeField = new TextField("75");
        minecraftTimeField.setEditable(false);
        
        Label chromeLabel = new Label("Chrome time limit (minutes):");
        TextField chromeTimeField = new TextField("75");
        chromeTimeField.setEditable(false);
        
        Label warningLabel = new Label("Warning time before limit (minutes):");
        TextField warningTimeField = new TextField("5");
        warningTimeField.setEditable(false);
        
        Button editButton = new Button("🔓 Edit Settings");
        editButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        
        Button blockChromeButton = new Button("🚫 Block Chrome (3h)");
        blockChromeButton.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white;");
        
        Button startButton = new Button("Start Monitoring");
        Button stopButton = new Button("Stop Monitoring");
        stopButton.setDisable(true);
        
        // Make control buttons require authentication
        startButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
        stopButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);
        
        // Edit button functionality
        editButton.setOnAction(e -> {
            if (securityManager.authenticate(primaryStage, "modify application settings")) {
                boolean editing = minecraftTimeField.isEditable();
                if (editing) {
                    // Save and lock
                    minecraftTimeField.setEditable(false);
                    chromeTimeField.setEditable(false);
                    warningTimeField.setEditable(false);
                    editButton.setText("🔓 Edit Settings");
                    editButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
                    logArea.appendText("Settings locked.\n");
                } else {
                    // Unlock for editing
                    minecraftTimeField.setEditable(true);
                    chromeTimeField.setEditable(true);
                    warningTimeField.setEditable(true);
                    editButton.setText("🔒 Lock Settings");
                    editButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
                    logArea.appendText("Settings unlocked for editing.\n");
                }
            }
        });
        
        startButton.setOnAction(e -> {
            // Start monitoring should NOT require authentication
            try {
                int minecraftLimit = Integer.parseInt(minecraftTimeField.getText());
                int chromeLimit = Integer.parseInt(chromeTimeField.getText());
                int warningTime = Integer.parseInt(warningTimeField.getText());
                
                timeTracker.setTimeLimit("minecraft.exe", minecraftLimit * 60);
                timeTracker.setTimeLimit("chrome.exe", chromeLimit * 60);
                timeTracker.setWarningTime(warningTime * 60);
                
                processMonitor.startMonitoring(timeTracker, voiceNotifier);
                
                startButton.setDisable(true);
                stopButton.setDisable(false);
                editButton.setDisable(true);
                
                logArea.appendText("🟢 Monitoring started...\n");
                logger.info("Monitoring started with limits - Minecraft: {}min, Chrome: {}min", 
                          minecraftLimit, chromeLimit);
                
            } catch (NumberFormatException ex) {
                showErrorDialog("Please enter valid numbers for time limits");
            }
        });
        
        stopButton.setOnAction(e -> {
            if (securityManager.authenticate(primaryStage, "stop monitoring")) {
                processMonitor.stopMonitoring();
                startButton.setDisable(false);
                stopButton.setDisable(true);
                editButton.setDisable(false);
                logArea.appendText("🔴 Monitoring stopped by administrator.\n");
                logger.info("Monitoring stopped");
            }
        });
        
        // Block Chrome button functionality
        blockChromeButton.setOnAction(e -> {
            if (securityManager.authenticate(primaryStage, "manually block Chrome for 3 hours")) {
                if (processMonitor.getApplicationBlocker() != null) {
                    processMonitor.getApplicationBlocker().blockChromeFor3Hours();
                    logArea.appendText("🚫 Chrome blocked for 3 hours by administrator.\n");
                    updateBlockedApplicationsStatus(logArea);
                } else {
                    showErrorDialog("Application blocker not initialized. Please start monitoring first.");
                }
            }
        });
        
        root.getChildren().addAll(
            menuBar,
            titleLabel,
            securityLabel,
            minecraftLabel, minecraftTimeField,
            chromeLabel, chromeTimeField,
            warningLabel, warningTimeField,
            editButton,
            blockChromeButton,
            startButton, stopButton,
            new Label("Log:"),
            logArea
        );
        
        Scene scene = new Scene(root, 450, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Prevent unauthorized closing
        primaryStage.setOnCloseRequest(e -> {
            e.consume(); // Always prevent default close
            
            // Show warning first
            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
            warningAlert.setTitle("Protected Application");
            warningAlert.setHeaderText("🔒 Administrator Authentication Required");
            warningAlert.setContentText("This application is protected. You need administrator access to close it.\n\nClick OK to authenticate or Cancel to minimize to system tray.");
            warningAlert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            
            warningAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // User wants to authenticate and close
                    if (securityManager.authenticate(primaryStage, "close the protected application")) {
                        // Stop monitoring first
                        if (processMonitor.isMonitoring()) {
                            processMonitor.stopMonitoring();
                            logger.info("Monitoring stopped before application exit");
                        }
                        Platform.exit();
                        System.exit(0);
                    }
                } else {
                    // User cancelled, hide to system tray
                    logger.info("Application close cancelled, hiding to system tray");
                    primaryStage.hide();
                    if (trayIcon != null) {
                        trayIcon.displayMessage("🔒 App Time Limiter", 
                            "Application is protected and running in background. Admin access required to close.", 
                            TrayIcon.MessageType.INFO);
                    }
                }
            });
        });
    }
    
    private void updateBlockedApplicationsStatus(TextArea logArea) {
        if (processMonitor.getApplicationBlocker() != null) {
            String status = processMonitor.getApplicationBlocker().getBlockedApplicationsStatus();
            if (!status.equals("No applications currently blocked")) {
                logArea.appendText("📋 " + status);
            }
        }
    }
    
    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
