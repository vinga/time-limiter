package com.apptimer.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apptimer.controller.MainApplicationController;

/**
 * Main window class that constructs the complete UI
 */
public class MainWindow {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
    private final Stage primaryStage;
    private final MainApplicationController controller;
    private final WindowManager windowManager;
    private final MenuBarManager menuManager;
    
    // UI Components
    private TextArea logArea;
    private TextField minecraftTimeField;
    private TextField chromeTimeField;
    private TextField warningTimeField;
    private TextField minecraftDelayField;
    private TextField chromeDelayField;
    private Label minecraftStatusLabel;
    private Label chromeStatusLabel;
    private Button startButton;
    private Button stopButton;
    private Button editButton;
    private VBox manualBlockingSection;
    
    public MainWindow(Stage primaryStage, MainApplicationController controller) {
        this.primaryStage = primaryStage;
        this.controller = controller;
        this.logArea = new TextArea(); // Initialize early for MenuBarManager
        this.windowManager = new WindowManager(primaryStage, controller);
        this.menuManager = new MenuBarManager(primaryStage, controller, logArea);
    }
    
    /**
     * Create and show the main application window
     */
    public void createAndShow() {
        // Initialize UI components
        initializeUIComponents();
        
        // Create left side with controls
        VBox leftSide = createLeftSide();
        
        // Create right side with log
        VBox rightSide = createRightSide();
        
        // Create the main window
        windowManager.createMainWindow(leftSide, rightSide, menuManager);
        
        // Setup event handlers
        setupEventHandlers();
        
        // Start auto-refresh
        controller.startAutoRefresh(minecraftStatusLabel, chromeStatusLabel);
        
        logger.info("Main window created and configured");
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeUIComponents() {
        // Initialize fields and components
        minecraftTimeField = new TextField();
        chromeTimeField = new TextField();
        warningTimeField = new TextField();
        minecraftDelayField = new TextField();
        chromeDelayField = new TextField();
        minecraftStatusLabel = new Label();
        chromeStatusLabel = new Label();
        startButton = new Button("Resume Monitoring");
        stopButton = new Button("Pause Monitoring");
        editButton = new Button("💾 Update Settings");
        
        // Load current settings into fields
        var settings = controller.getSettingsManager().getCurrentSettings();
        minecraftTimeField.setText(String.valueOf(settings.minecraftLimit));
        chromeTimeField.setText(String.valueOf(settings.chromeLimit));
        warningTimeField.setText(String.valueOf(settings.warningTime));
        minecraftDelayField.setText(String.valueOf(settings.minecraftDelay));
        chromeDelayField.setText(String.valueOf(settings.chromeDelay));
        
        // Set initial button states
        startButton.setDisable(true); // Already auto-started
        stopButton.setDisable(false);
        
        logger.info("UI components initialized with current settings");
    }
    
    /**
     * Create left side with all control sections
     */
    private VBox createLeftSide() {
        VBox leftSide = new VBox(10);
        leftSide.setPadding(new Insets(15));
        leftSide.setPrefWidth(580);
        
        // Create all sections
        VBox timeLimitsSection = UISectionsFactory.createTimeLimitsSection(
            minecraftTimeField, chromeTimeField, warningTimeField, 
            minecraftStatusLabel, chromeStatusLabel, minecraftDelayField,
            chromeDelayField, editButton);
        
        manualBlockingSection = UISectionsFactory.createManualBlockingSection();
        
        VBox systemControlsSection = UISectionsFactory.createSystemControlsSection(
            startButton, stopButton);
        
        // Add all sections to left side (without block delays section)
        leftSide.getChildren().addAll(
            timeLimitsSection,
            manualBlockingSection,
            systemControlsSection
        );
        
        return leftSide;
    }
    
    /**
     * Create right side with log area
     */
    private VBox createRightSide() {
        VBox rightSide = new VBox(10);
        rightSide.setPadding(new Insets(15));
        
        VBox logSection = UISectionsFactory.createLogSection(logArea);
        VBox statusSection = UISectionsFactory.createStatusSection(
            minecraftStatusLabel, chromeStatusLabel);
        
        rightSide.getChildren().addAll(logSection, statusSection);
        
        return rightSide;
    }
    
    /**
     * Setup event handlers for UI components
     */
    private void setupEventHandlers() {
        // Create event handlers instance
        EventHandlers eventHandlers = new EventHandlers(
            primaryStage, 
            controller.getProcessMonitor(),
            controller.getTimeTracker(),
            controller.getVoiceNotifier(),
            controller.getSecurityManager(),
            controller.getSettingsManager(),
            controller.getTrayManager(),
            logArea,
            minecraftTimeField,
            chromeTimeField, 
            warningTimeField,
            minecraftDelayField,
            chromeDelayField
        );
        
        // Setup button event handlers
        editButton.setOnAction(e -> eventHandlers.handleEditButtonClick());
        
        startButton.setOnAction(e -> {
            eventHandlers.handleStartMonitoring();
            startButton.setDisable(true);
            stopButton.setDisable(false);
        });
        
        stopButton.setOnAction(e -> {
            eventHandlers.handleStopMonitoring();
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });
        
        // Setup manual blocking handlers
        if (manualBlockingSection.getUserData() instanceof UISectionsFactory.ManualBlockingControls) {
            UISectionsFactory.ManualBlockingControls controls = 
                (UISectionsFactory.ManualBlockingControls) manualBlockingSection.getUserData();
            
            controls.blockMinecraftButton.setOnAction(e -> 
                eventHandlers.handleManualBlock("minecraft.exe", controls.blockMinecraftMinutesField));
            
            controls.blockChromeButton.setOnAction(e -> 
                eventHandlers.handleManualBlock("chrome.exe", controls.blockChromeMinutesField));
        }
        
        // Setup system controls handlers
        Button exitButton = new Button("🔒 Exit Application (Admin Required)");
        
        exitButton.setOnAction(e -> eventHandlers.handleExitApplication());
        
        logger.info("Event handlers configured");
    }
    
    /**
     * Get the log area for external access
     */
    public TextArea getLogArea() {
        return logArea;
    }
    
    /**
     * Get the window manager
     */
    public WindowManager getWindowManager() {
        return windowManager;
    }
}
