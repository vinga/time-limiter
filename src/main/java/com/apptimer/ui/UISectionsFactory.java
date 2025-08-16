package com.apptimer.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating UI sections with consistent styling
 */
public class UISectionsFactory {
    private static final Logger logger = LoggerFactory.getLogger(UISectionsFactory.class);
    
    /**
     * Create header section with title and security status
     */
    public static VBox createHeaderSection() {
        VBox section = new VBox(5);
        section.setStyle("-fx-border-color: #2196f3; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 8; -fx-background-color: #e3f2fd; -fx-pref-height: 65;");
        
        Label titleLabel = new Label("🔒 Enhanced App Time Limiter");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        
        Label statusLabel = new Label("🛡️ Enhanced Security Mode - Auto-monitoring enabled");
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #388e3c;");
        
        section.getChildren().addAll(titleLabel, statusLabel);
        return section;
    }
    
    /**
     * Create time limits configuration section
     */
    public static VBox createTimeLimitsSection(TextField minecraftTimeField, TextField chromeTimeField, 
                                              TextField warningTimeField, Label minecraftStatusLabel, 
                                              Label chromeStatusLabel, TextField minecraftDelayField,
                                              TextField chromeDelayField, Button editButton) {
        VBox section = new VBox(8);
        section.setStyle("-fx-border-color: #ff9800; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #fff3e0; -fx-pref-height: 180;");
        
        Label sectionTitle = new Label("⏱️ TIME LIMITS & BLOCK DELAYS CONFIGURATION");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #f57c00;");
        
        // Make fields always editable and set preferred width
        minecraftTimeField.setEditable(true);
        minecraftTimeField.setPrefWidth(80);
        chromeTimeField.setEditable(true);
        chromeTimeField.setPrefWidth(80);
        warningTimeField.setEditable(true);
        warningTimeField.setPrefWidth(80);
        minecraftDelayField.setEditable(true);
        minecraftDelayField.setPrefWidth(80);
        chromeDelayField.setEditable(true);
        chromeDelayField.setPrefWidth(80);
        
        GridPane timeLimitsGrid = new GridPane();
        timeLimitsGrid.setHgap(10);
        timeLimitsGrid.setVgap(6);
        
        timeLimitsGrid.add(new Label("Minecraft limit (minutes):"), 0, 0);
        timeLimitsGrid.add(minecraftTimeField, 1, 0);
        timeLimitsGrid.add(minecraftStatusLabel, 2, 0);
        
        timeLimitsGrid.add(new Label("Chrome limit (minutes):"), 0, 1);
        timeLimitsGrid.add(chromeTimeField, 1, 1);
        timeLimitsGrid.add(chromeStatusLabel, 2, 1);
        
        timeLimitsGrid.add(new Label("Warning time (minutes):"), 0, 2);
        timeLimitsGrid.add(warningTimeField, 1, 2);
        
        timeLimitsGrid.add(new Label("Minecraft block delay (minutes):"), 0, 3);
        timeLimitsGrid.add(minecraftDelayField, 1, 3);
        
        timeLimitsGrid.add(new Label("Chrome block delay (minutes):"), 0, 4);
        timeLimitsGrid.add(chromeDelayField, 1, 4);
        
        editButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-font-size: 12px;");
        
        section.getChildren().addAll(sectionTitle, timeLimitsGrid, editButton);
        return section;
    }
    
    
    /**
     * Create manual blocking controls section
     */
    public static VBox createManualBlockingSection() {
        VBox section = new VBox(8);
        section.setStyle("-fx-border-color: #f44336; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #ffebee; -fx-pref-height: 85;");
        
        Label sectionTitle = new Label("🚫 MANUAL BLOCKING CONTROLS");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #d32f2f;");
        
        Label warningLabel = new Label("⚠️ Requires administrator authentication");
        warningLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #d32f2f;");
        
        GridPane blockingGrid = new GridPane();
        blockingGrid.setHgap(10);
        blockingGrid.setVgap(6);
        
        Button blockMinecraftButton = new Button("🚫 Block Minecraft");
        TextField blockMinecraftMinutesField = new TextField("180");
        blockMinecraftButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; -fx-font-size: 11px;");
        blockMinecraftMinutesField.setPrefWidth(80);
        
        Button blockChromeButton = new Button("🚫 Block Chrome");
        TextField blockChromeMinutesField = new TextField("180");
        blockChromeButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; -fx-font-size: 11px;");
        blockChromeMinutesField.setPrefWidth(80);
        
        blockingGrid.add(blockMinecraftButton, 0, 0);
        blockingGrid.add(blockMinecraftMinutesField, 1, 0);
        blockingGrid.add(new Label("minutes"), 2, 0);
        
        blockingGrid.add(blockChromeButton, 0, 1);
        blockingGrid.add(blockChromeMinutesField, 1, 1);
        blockingGrid.add(new Label("minutes"), 2, 1);
        
        section.getChildren().addAll(sectionTitle, warningLabel, blockingGrid);
        
        // Store references for event handlers
        section.setUserData(new ManualBlockingControls(blockMinecraftButton, blockMinecraftMinutesField, 
                                                      blockChromeButton, blockChromeMinutesField));
        
        return section;
    }
    
    /**
     * Create system controls section
     */
    public static VBox createSystemControlsSection(Button startButton, Button stopButton) {
        VBox section = new VBox(8);
        section.setStyle("-fx-border-color: #4caf50; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #e8f5e8; -fx-pref-height: 110;");
        
        Label sectionTitle = new Label("⚙️ SYSTEM CONTROLS");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #388e3c;");
        
        Button exitButton = new Button("🔒 Exit Application (Admin Required)");
        
        startButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 180; -fx-padding: 8 12; -fx-font-size: 11px;");
        stopButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 180; -fx-padding: 8 12; -fx-font-size: 11px;");
        exitButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 180; -fx-padding: 8 12; -fx-font-size: 11px;");
        
        // Set initial states
        startButton.setDisable(true); // Already auto-started
        stopButton.setDisable(false);
        
        section.getChildren().addAll(sectionTitle, startButton, stopButton, exitButton);
        
        // Store references for event handlers
        section.setUserData(new SystemControls(null, exitButton));
        
        return section;
    }
    
    /**
     * Create status monitoring section
     */
    public static VBox createStatusSection(Label minecraftStatusLabel, Label chromeStatusLabel) {
        VBox section = new VBox(6);
        section.setStyle("-fx-border-color: #4caf50; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #e8f5e8; -fx-pref-height: 100; -fx-pref-width: 560;");
        
        Label sectionTitle = new Label("📊 CURRENT STATUS");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #388e3c;");
        
        Label monitoringStatus = new Label("🟢 Monitoring: ACTIVE");
        monitoringStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #388e3c; -fx-font-weight: bold;");
        
        section.getChildren().addAll(sectionTitle, monitoringStatus, minecraftStatusLabel, chromeStatusLabel);
        return section;
    }
    
    /**
     * Create activity log section
     */
    public static VBox createLogSection(TextArea logArea) {
        VBox section = new VBox(8);
        section.setStyle("-fx-border-color: #607d8b; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #eceff1;");
        
        Label sectionTitle = new Label("📝 ACTIVITY LOG");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #455a64;");
        
        logArea.setEditable(false);
        logArea.setPrefRowCount(18);
        logArea.setPrefWidth(560);
        logArea.setPrefHeight(380);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px; -fx-background-color: #fafafa;");
        
        section.getChildren().addAll(sectionTitle, logArea);
        return section;
    }
    
    // Helper classes to store control references
    public static class ManualBlockingControls {
        public final Button blockMinecraftButton;
        public final TextField blockMinecraftMinutesField;
        public final Button blockChromeButton;
        public final TextField blockChromeMinutesField;
        
        public ManualBlockingControls(Button blockMinecraftButton, TextField blockMinecraftMinutesField,
                                     Button blockChromeButton, TextField blockChromeMinutesField) {
            this.blockMinecraftButton = blockMinecraftButton;
            this.blockMinecraftMinutesField = blockMinecraftMinutesField;
            this.blockChromeButton = blockChromeButton;
            this.blockChromeMinutesField = blockChromeMinutesField;
        }
    }
    
    public static class SystemControls {
        public final Button hideButton;
        public final Button exitButton;
        
        public SystemControls(Button hideButton, Button exitButton) {
            this.hideButton = hideButton;
            this.exitButton = exitButton;
        }
    }
}
