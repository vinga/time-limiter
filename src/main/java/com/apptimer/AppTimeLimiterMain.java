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
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private ScheduledExecutorService protectionService;
    private volatile boolean isProtectionActive = false;
    
    // Settings persistence
    private static final String SETTINGS_FILE = "app-time-limiter-settings.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Default settings
    private int defaultMinecraftLimit = 75; // minutes
    private int defaultChromeLimit = 75;     // minutes
    private int defaultWarningTime = 5;      // minutes
    
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
            "• Monitoring: AUTO-STARTS on app launch\n" +
            "• Password required for:\n" +
            "  - Modifying time limits\n" +
            "  - Stopping monitoring\n" +
            "  - Closing the application\n" +
            "  - System tray exit\n" +
            "• No password required for:\n" +
            "  - Restarting monitoring\n" +
            "• Authentication timeout: 30 minutes\n" +
            "• All attempts are logged\n" +
            "• App auto-minimizes to system tray\n\n" +
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
            
            // Add shutdown hook to save settings
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Application shutting down, saving settings...");
                saveSettings();
            }));
            
            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorDialog("Failed to start application: " + e.getMessage());
        }
    }
    
    private HBox createBlockChromeRow(Button blockButton, TextField minutesField, Label minutesLabel) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(blockButton, minutesField, minutesLabel);
        return row;
    }
    
    private void updateRemainingTimeDisplay(TextArea logArea) {
        if (timeTracker != null) {
            logArea.appendText("⏱️ Time status:\n");
            
            // Show Minecraft status
            long minecraftUsed = timeTracker.getTotalTime("minecraft.exe") / 60; // Convert to minutes
            long minecraftLimit = defaultMinecraftLimit;
            long minecraftRemaining = Math.max(0, minecraftLimit - minecraftUsed);
            
            logArea.appendText(String.format("   Minecraft: %d/%d minutes used (%d remaining)\n", 
                             minecraftUsed, minecraftLimit, minecraftRemaining));
            
            // Show Chrome status  
            long chromeUsed = timeTracker.getTotalTime("chrome.exe") / 60; // Convert to minutes
            long chromeLimit = defaultChromeLimit;
            long chromeRemaining = Math.max(0, chromeLimit - chromeUsed);
            
            logArea.appendText(String.format("   Chrome: %d/%d minutes used (%d remaining)\n", 
                             chromeUsed, chromeLimit, chromeRemaining));
        }
    }
    
    private void startAutoRefresh(TextArea logArea, Label minecraftStatusLabel, Label chromeStatusLabel) {
        protectionService.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                if (processMonitor != null && processMonitor.isMonitoring()) {
                    updateStatusLabels(minecraftStatusLabel, chromeStatusLabel);
                    // Occasionally update log too (every 5 minutes)
                    if (System.currentTimeMillis() % (5 * 60 * 1000) < 60000) {
                        logArea.appendText("\n🔄 === TIME STATUS UPDATE ===\n");
                        updateRemainingTimeDisplay(logArea);
                        updateBlockedApplicationsStatus(logArea);
                    }
                }
            });
        }, 60, 60, TimeUnit.SECONDS); // Every 60 seconds
        
        // Auto-save time usage every 5 minutes
        protectionService.scheduleAtFixedRate(() -> {
            saveSettings();
        }, 300, 300, TimeUnit.SECONDS); // Every 5 minutes
        
        logger.info("Auto-refresh and auto-save started");
    }
    
    private void updateStatusLabels(Label minecraftStatusLabel, Label chromeStatusLabel) {
        if (timeTracker != null) {
            // Update Minecraft status
            long minecraftUsed = timeTracker.getTotalTime("minecraft.exe") / 60;
            long minecraftLimit = defaultMinecraftLimit;
            long minecraftRemaining = Math.max(0, minecraftLimit - minecraftUsed);
            
            minecraftStatusLabel.setText(String.format("⏱️ Minecraft: %d/%d min (%d remaining)", 
                                        minecraftUsed, minecraftLimit, minecraftRemaining));
            
            // Update Chrome status
            long chromeUsed = timeTracker.getTotalTime("chrome.exe") / 60;
            long chromeLimit = defaultChromeLimit;
            long chromeRemaining = Math.max(0, chromeLimit - chromeUsed);
            
            chromeStatusLabel.setText(String.format("⏱️ Chrome: %d/%d min (%d remaining)", 
                                    chromeUsed, chromeLimit, chromeRemaining));
        }
    }
    
    private void initializeComponents() {
        processMonitor = new ProcessMonitor();
        timeTracker = new TimeTracker();
        voiceNotifier = new VoiceNotifier();
        securityManager = new SecurityManager();
        protectionService = Executors.newScheduledThreadPool(1);
        
        // Load saved settings
        loadSettings();
        
        // AUTO-START MONITORING with loaded/default settings
        startMonitoringWithDefaults();
        
        // Start protection service
        startProtectionService();
    }
    
    private void startMonitoringWithDefaults() {
        // Set time limits using loaded/default settings
        timeTracker.setTimeLimit("minecraft.exe", defaultMinecraftLimit * 60);
        timeTracker.setTimeLimit("chrome.exe", defaultChromeLimit * 60);
        timeTracker.setWarningTime(defaultWarningTime * 60);
        
        // Start monitoring in background
        processMonitor.startMonitoring(timeTracker, voiceNotifier);
        
        logger.info("Monitoring auto-started with saved settings - Minecraft: {}min, Chrome: {}min, Warning: {}min", 
                   defaultMinecraftLimit, defaultChromeLimit, defaultWarningTime);
    }
    
    private void loadSettings() {
        try {
            Path settingsPath = Paths.get(SETTINGS_FILE);
            if (Files.exists(settingsPath)) {
                String content = Files.readString(settingsPath);
                JsonNode settings = objectMapper.readTree(content);
                
                defaultMinecraftLimit = settings.path("minecraftLimit").asInt(75);
                defaultChromeLimit = settings.path("chromeLimit").asInt(75);
                defaultWarningTime = settings.path("warningTime").asInt(5);
                
                // Load password hash if exists
                String passwordHash = settings.path("passwordHash").asText("");
                if (!passwordHash.isEmpty() && securityManager != null) {
                    securityManager.setPasswordHash(passwordHash);
                }
                
                // Load time usage data if from today
                String lastSaveDate = settings.path("lastSaveDate").asText("");
                String today = java.time.LocalDate.now().toString();
                
                if (today.equals(lastSaveDate) && settings.has("timeUsage")) {
                    JsonNode timeUsage = settings.get("timeUsage");
                    timeUsage.fieldNames().forEachRemaining(processName -> {
                        long timeSeconds = timeUsage.get(processName).asLong(0);
                        if (timeTracker != null) {
                            timeTracker.setTotalTime(processName, timeSeconds);
                        }
                    });
                    logger.info("Time usage restored from today's session");
                } else {
                    logger.info("Starting fresh time tracking (new day or no previous data)");
                }
                
                logger.info("Settings loaded from file: Minecraft={}min, Chrome={}min, Warning={}min", 
                           defaultMinecraftLimit, defaultChromeLimit, defaultWarningTime);
            } else {
                logger.info("No settings file found, using defaults");
            }
        } catch (Exception e) {
            logger.error("Error loading settings, using defaults", e);
        }
    }
    
    private void saveSettings() {
        try {
            ObjectNode settings = objectMapper.createObjectNode();
            settings.put("minecraftLimit", defaultMinecraftLimit);
            settings.put("chromeLimit", defaultChromeLimit);
            settings.put("warningTime", defaultWarningTime);
            
            // Save password hash
            if (securityManager != null) {
                settings.put("passwordHash", securityManager.getPasswordHash());
            }
            
            // Save time usage data
            if (timeTracker != null) {
                ObjectNode timeUsage = objectMapper.createObjectNode();
                Map<String, Long> allTimes = timeTracker.getAllTotalTimes();
                for (Map.Entry<String, Long> entry : allTimes.entrySet()) {
                    timeUsage.put(entry.getKey(), entry.getValue());
                }
                settings.set("timeUsage", timeUsage);
                
                // Save current date to reset daily
                settings.put("lastSaveDate", java.time.LocalDate.now().toString());
            }
            
            String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings);
            Files.writeString(Paths.get(SETTINGS_FILE), content);
            
            logger.info("Settings and time usage saved to file");
        } catch (Exception e) {
            logger.error("Error saving settings", e);
        }
    }
    
    private void startProtectionService() {
        // Remove the always-on-top and visibility restoration during monitoring
        protectionService.scheduleAtFixedRate(() -> {
            if (isProtectionActive) {
                // Just log that protection is active, don't force window visibility
                logger.debug("Protection service active - monitoring: " + 
                           (processMonitor != null ? processMonitor.isMonitoring() : false));
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        logger.info("Protection service started");
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
            
            // Create initial popup menu
            PopupMenu popup = createTrayPopupMenu();
            
            trayIcon = new TrayIcon(image, "🔒 App Time Limiter - Kid-Safe Mode (Active)", popup);
            trayIcon.setImageAutoSize(true);
            
            // Add double-click handler to show application
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                logger.info("System tray icon double-clicked");
                toggleWindowVisibility();
            }));
            
            systemTray.add(trayIcon);
            logger.info("System tray icon created");
            
        } catch (AWTException e) {
            logger.error("Failed to create system tray icon", e);
        }
    }
    
    private PopupMenu createTrayPopupMenu() {
        PopupMenu popup = new PopupMenu();
        
        java.awt.MenuItem showItem = new java.awt.MenuItem("Show Window");
        showItem.addActionListener(e -> {
            logger.info("*** SYSTEM TRAY: Show Window clicked ***");
            Platform.runLater(() -> {
                logger.info("Platform.runLater executed for Show Window");
                showWindow();
            });
        });
        
        java.awt.MenuItem hideItem = new java.awt.MenuItem("Hide to Tray");
        hideItem.addActionListener(e -> {
            logger.info("*** SYSTEM TRAY: Hide to Tray clicked ***");
            Platform.runLater(() -> {
                logger.info("Platform.runLater executed for Hide to Tray");
                hideWindow();
            });
        });
        
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
        
        // Add both show and hide for testing
        popup.add(showItem);
        popup.add(hideItem);
        popup.addSeparator();
        popup.add(exitItem);
        
        return popup;
    }
    
    private void showWindow() {
        if (primaryStage != null) {
            logger.info("Showing window from tray (unminimize)");
            
            try {
                // Restore from minimized state
                if (primaryStage.isIconified()) {
                    primaryStage.setIconified(false);
                }
                
                // Ensure window is visible and focused
                primaryStage.toFront();
                primaryStage.requestFocus();
                
                logger.info("Window restored successfully");
                
            } catch (Exception e) {
                logger.error("Error showing window", e);
            }
        } else {
            logger.error("primaryStage is null!");
        }
    }
    
    private void hideWindow() {
        if (primaryStage != null) {
            logger.info("Hiding window to tray (minimize)");
            
            try {
                // Minimize to system tray
                primaryStage.setIconified(true);
                logger.info("Window minimized successfully");
                
            } catch (Exception e) {
                logger.error("Error hiding window", e);
            }
        } else {
            logger.error("primaryStage is null!");
        }
    }
    
    private void toggleWindowVisibility() {
        if (primaryStage != null) {
            if (primaryStage.isShowing() && !primaryStage.isIconified()) {
                logger.info("Window is showing, hiding it");
                hideWindow();
            } else {
                logger.info("Window is hidden or minimized, showing it");
                showWindow();
            }
        }
    }
    
    private void createMainWindow(Stage primaryStage) {
        primaryStage.setTitle("🔒 App Time Limiter - Kid-Safe Mode");
        
        // PROTECTION: Hide from taskbar, make accessible only via system tray
        primaryStage.setResizable(false);
        
        BorderPane root = new BorderPane();
        
        // Left side - main controls
        VBox leftSide = new VBox(10);
        leftSide.setPadding(new Insets(20));
        leftSide.setPrefWidth(400);
        
        // Create menu bar with security options
        MenuBar menuBar = createMenuBar();
        
        Label titleLabel = new Label("Application Time Limiter");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label securityLabel = new Label("🔐 Kid-Safe Mode - Running in background (check system tray)");
        securityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        
        // Time limit controls - load from saved settings
        Label minecraftLabel = new Label("Minecraft time limit (minutes):");
        TextField minecraftTimeField = new TextField(String.valueOf(defaultMinecraftLimit));
        minecraftTimeField.setEditable(false);
        
        Label chromeLabel = new Label("Chrome time limit (minutes):");
        TextField chromeTimeField = new TextField(String.valueOf(defaultChromeLimit));
        chromeTimeField.setEditable(false);
        
        Label warningLabel = new Label("Warning time before limit (minutes):");
        TextField warningTimeField = new TextField(String.valueOf(defaultWarningTime));
        warningTimeField.setEditable(false);
        
        // Status labels for remaining time
        Label minecraftStatusLabel = new Label("⏱️ Minecraft: Loading...");
        minecraftStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4caf50;");
        
        Label chromeStatusLabel = new Label("⏱️ Chrome: Loading...");
        chromeStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4caf50;");
        
        Button editButton = new Button("🔓 Edit Settings");
        editButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        
        // Edit button always enabled - password protection provides security
        
        Button blockMinecraftButton = new Button("🚫 Block Minecraft");
        blockMinecraftButton.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white;");
        
        TextField blockMinecraftMinutesField = new TextField("180");
        blockMinecraftMinutesField.setPrefWidth(60);
        blockMinecraftMinutesField.setPromptText("Minutes");
        
        Label blockMinecraftMinutesLabel = new Label("minutes");
        
        Button blockChromeButton = new Button("🚫 Block Chrome");
        blockChromeButton.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white;");
        
        TextField blockChromeMinutesField = new TextField("180");
        blockChromeMinutesField.setPrefWidth(60);
        blockChromeMinutesField.setPromptText("Minutes");
        
        Label chromeMinutesLabel = new Label("minutes");
        
        Button exitAppButton = new Button("🔒 Exit Application (Admin Only)");
        exitAppButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // DEBUG BUTTONS - Only keep the working approach
        Button debugHideButton = new Button("🔍 Hide to Tray (Minimize)");
        debugHideButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        
        Button startButton = new Button("Start Monitoring");
        Button stopButton = new Button("Stop Monitoring");
        
        // Monitoring is auto-started, so disable start and enable stop
        startButton.setDisable(true);
        stopButton.setDisable(false);
        
        // Make control buttons require authentication
        startButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
        stopButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(25);
        logArea.setPrefWidth(350);
        
        // Right side - log area
        VBox rightSide = new VBox(10);
        rightSide.setPadding(new Insets(20));
        Label logLabel = new Label("Log:");
        logLabel.setStyle("-fx-font-weight: bold;");
        rightSide.getChildren().addAll(logLabel, logArea);
        
        // Show initial status that monitoring auto-started
        logArea.appendText(String.format("🟢 Monitoring auto-started: Minecraft=%dmin, Chrome=%dmin, Warning=%dmin\n", 
                          defaultMinecraftLimit, defaultChromeLimit, defaultWarningTime));
        logArea.appendText("🔒 App running in kid-safe mode - minimized to system tray\n");
        
        // Show remaining time for applications
        updateRemainingTimeDisplay(logArea);
        
        // Update status labels initially
        updateStatusLabels(minecraftStatusLabel, chromeStatusLabel);
        
        // Auto-refresh time status every minute
        startAutoRefresh(logArea, minecraftStatusLabel, chromeStatusLabel);
        
        // Edit button functionality - real-time settings changes
        editButton.setOnAction(e -> {
            if (securityManager.authenticate(primaryStage, "modify application settings")) {
                boolean editing = minecraftTimeField.isEditable();
                if (editing) {
                    // Save and lock - apply changes immediately to active monitoring
                    try {
                        int minecraftLimit = Integer.parseInt(minecraftTimeField.getText());
                        int chromeLimit = Integer.parseInt(chromeTimeField.getText());
                        int warningTime = Integer.parseInt(warningTimeField.getText());
                        
                        // Apply changes to active monitoring immediately
                        timeTracker.setTimeLimit("minecraft.exe", minecraftLimit * 60);
                        timeTracker.setTimeLimit("chrome.exe", chromeLimit * 60);
                        timeTracker.setWarningTime(warningTime * 60);
                        
                        // Update stored settings and save to file
                        defaultMinecraftLimit = minecraftLimit;
                        defaultChromeLimit = chromeLimit;
                        defaultWarningTime = warningTime;
                        saveSettings();
                        
                        minecraftTimeField.setEditable(false);
                        chromeTimeField.setEditable(false);
                        warningTimeField.setEditable(false);
                        editButton.setText("🔓 Edit Settings");
                        editButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
                        
                        logArea.appendText("⚙️ Settings updated and applied to active monitoring\n");
                        logger.info("Settings updated during monitoring - Minecraft: {}min, Chrome: {}min, Warning: {}min", 
                                  minecraftLimit, chromeLimit, warningTime);
                        
                    } catch (NumberFormatException ex) {
                        showErrorDialog("Please enter valid numbers for time limits");
                        return; // Don't lock fields if there's an error
                    }
                } else {
                    // Unlock for editing
                    minecraftTimeField.setEditable(true);
                    chromeTimeField.setEditable(true);
                    warningTimeField.setEditable(true);
                    editButton.setText("🔒 Save Settings");
                    editButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
                    logArea.appendText("✏️ Settings unlocked for editing...\n");
                }
            }
        });
        
        startButton.setOnAction(e -> {
            // Restart monitoring with current UI settings (no authentication required)
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
                
                logArea.appendText("🔄 Monitoring restarted with updated settings...\n");
                logger.info("Monitoring restarted with limits - Minecraft: {}min, Chrome: {}min", 
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
                logArea.appendText("🔴 Monitoring stopped by administrator.\n");
                logger.info("Monitoring stopped");
            }
        });
        
        // Block Chrome button functionality
        blockChromeButton.setOnAction(e -> {
            if (securityManager.authenticate(primaryStage, "manually block Chrome")) {
                try {
                    int minutes = Integer.parseInt(blockChromeMinutesField.getText());
                    if (minutes <= 0 || minutes > 1440) { // Max 24 hours = 1440 minutes
                        showErrorDialog("Please enter a valid number of minutes (1-1440)");
                        return;
                    }
                    
                    int hours = minutes / 60;
                    if (processMonitor.getApplicationBlocker() != null) {
                        processMonitor.getApplicationBlocker().blockApplicationMinutes("chrome.exe", minutes, "Manually blocked by administrator");
                        
                        if (minutes < 60) {
                            logArea.appendText(String.format("🚫 Chrome blocked for %d minutes by administrator.\n", minutes));
                        } else {
                            int remainingMinutes = minutes % 60;
                            if (remainingMinutes == 0) {
                                logArea.appendText(String.format("🚫 Chrome blocked for %d hours by administrator.\n", hours));
                            } else {
                                logArea.appendText(String.format("🚫 Chrome blocked for %dh %dm by administrator.\n", hours, remainingMinutes));
                            }
                        }
                        updateBlockedApplicationsStatus(logArea);
                        
                        // Show remaining block time
                        if (processMonitor.getApplicationBlocker() != null) {
                            long blockRemaining = processMonitor.getApplicationBlocker().getRemainingBlockTime("chrome.exe");
                            logArea.appendText(String.format("⏰ %d minutes remaining in block\n", blockRemaining));
                        }
                    } else {
                        showErrorDialog("Application blocker not initialized. Please start monitoring first.");
                    }
                } catch (NumberFormatException ex) {
                    showErrorDialog("Please enter a valid number of minutes");
                }
            }
        });
        
        // Block Minecraft button functionality
        blockMinecraftButton.setOnAction(e -> {
            if (securityManager.authenticate(primaryStage, "manually block Minecraft")) {
                try {
                    int minutes = Integer.parseInt(blockMinecraftMinutesField.getText());
                    if (minutes <= 0 || minutes > 1440) {
                        showErrorDialog("Please enter a valid number of minutes (1-1440)");
                        return;
                    }
                    
                    if (processMonitor.getApplicationBlocker() != null) {
                        processMonitor.getApplicationBlocker().blockApplicationMinutes("minecraft.exe", minutes, "Manually blocked by administrator");
                        
                        if (minutes < 60) {
                            logArea.appendText(String.format("🚫 Minecraft blocked for %d minutes by administrator.\n", minutes));
                        } else {
                            int hours = minutes / 60;
                            int remainingMinutes = minutes % 60;
                            if (remainingMinutes == 0) {
                                logArea.appendText(String.format("🚫 Minecraft blocked for %d hours by administrator.\n", hours));
                            } else {
                                logArea.appendText(String.format("🚫 Minecraft blocked for %dh %dm by administrator.\n", hours, remainingMinutes));
                            }
                        }
                        updateBlockedApplicationsStatus(logArea);
                        
                        if (processMonitor.getApplicationBlocker() != null) {
                            long blockRemaining = processMonitor.getApplicationBlocker().getRemainingBlockTime("minecraft.exe");
                            logArea.appendText(String.format("⏰ %d minutes remaining in block\n", blockRemaining));
                        }
                    } else {
                        showErrorDialog("Application blocker not initialized. Please start monitoring first.");
                    }
                } catch (NumberFormatException ex) {
                    showErrorDialog("Please enter a valid number of minutes");
                }
            }
        });
        
        // Exit application button functionality
        exitAppButton.setOnAction(e -> {
            if (securityManager.authenticate(primaryStage, "exit the protected application")) {
                // Stop monitoring first
                if (processMonitor.isMonitoring()) {
                    processMonitor.stopMonitoring();
                    logger.info("Monitoring stopped before application exit");
                }
                
                // Clean shutdown
                if (protectionService != null) {
                    protectionService.shutdown();
                }
                
                Platform.exit();
                System.exit(0);
            }
        });
        
        // WORKING HIDE/SHOW FUNCTIONALITY (using iconified)
        debugHideButton.setOnAction(e -> {
            logger.info("*** HIDE TO TRAY BUTTON CLICKED ***");
            hideWindow();
        });
        
        leftSide.getChildren().addAll(
            menuBar,
            titleLabel,
            securityLabel,
            minecraftLabel, minecraftTimeField, minecraftStatusLabel,
            chromeLabel, chromeTimeField, chromeStatusLabel,
            warningLabel, warningTimeField,
            editButton,
            createBlockChromeRow(blockMinecraftButton, blockMinecraftMinutesField, blockMinecraftMinutesLabel),
            createBlockChromeRow(blockChromeButton, blockChromeMinutesField, chromeMinutesLabel),
            startButton, stopButton,
            exitAppButton,
            new Label("Tray Controls:"),
            debugHideButton
        );
        
        // Set up the layout
        root.setLeft(leftSide);
        root.setRight(rightSide);
        
        Scene scene = new Scene(root, 800, 700);
        primaryStage.setScene(scene);
        
        // PROTECTION: Block Alt+F4 and other keyboard shortcuts
        scene.setOnKeyPressed(e -> {
            // Block Alt+F4 (common close shortcut)
            if (e.getCode() == KeyCode.F4 && e.isAltDown()) {
                e.consume();
                logger.info("Alt+F4 blocked (kid-safe mode)");
                if (trayIcon != null) {
                    trayIcon.displayMessage("🔒 Shortcut Blocked", 
                        "Alt+F4 is disabled. Use system tray to access app.", 
                        TrayIcon.MessageType.WARNING);
                }
            }
            // Block Ctrl+Q (common quit shortcut)
            if (e.getCode() == KeyCode.Q && e.isControlDown()) {
                e.consume();
                logger.info("Ctrl+Q blocked (kid-safe mode)");
            }
            // Block Ctrl+W (close window)
            if (e.getCode() == KeyCode.W && e.isControlDown()) {
                e.consume();
                logger.info("Ctrl+W blocked (kid-safe mode)");
            }
        });
        
        // START IN KID-SAFE MODE (auto-minimize to tray)
        // Show window normally and let it fully initialize
        primaryStage.show();
        
        // Enable protection immediately
        isProtectionActive = true;
        
        // Auto-minimize to tray after brief initialization delay
        Platform.runLater(() -> Platform.runLater(() -> {
            logger.info("Auto-minimizing to system tray for kid-safe mode");
            primaryStage.setIconified(true);
            
            if (trayIcon != null) {
                trayIcon.displayMessage("🔒 App Time Limiter Started", 
                    "Running in kid-safe mode. Right-click tray icon to access.", 
                    TrayIcon.MessageType.INFO);
            }
        }));
        
        // SIMPLIFIED PROTECTION FOR TESTING
        primaryStage.setOnCloseRequest(e -> {
            e.consume(); // Prevent default close
            logger.info("Close request intercepted - minimizing to system tray");
            
            // Use iconified instead of hide
            primaryStage.setIconified(true);
            
            if (trayIcon != null) {
                trayIcon.displayMessage("🔒 App Time Limiter Protected", 
                    "App minimized to system tray. Right-click tray icon to show.", 
                    TrayIcon.MessageType.INFO);
            }
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
