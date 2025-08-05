package com.apptimer;

import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apptimer.controller.MainApplicationController;
import com.apptimer.ui.MainWindow;

/**
 * Main application class for Enhanced App Time Limiter
 * Monitors and limits application usage time on Windows with enhanced security
 */
public class AppTimeLimiterMain extends Application {
    private static final Logger logger = LoggerFactory.getLogger(AppTimeLimiterMain.class);
    
    private MainApplicationController controller;
    private MainWindow mainWindow;
    
    public static void main(String[] args) {
        logger.info("Starting Enhanced App Time Limiter...");
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Initializing Enhanced App Time Limiter...");
            
            // Create main window first to get log area
            mainWindow = new MainWindow(primaryStage, null);
            
            // Initialize main application controller
            controller = new MainApplicationController(primaryStage, mainWindow.getLogArea());
            
            // Update main window with controller reference
            mainWindow = new MainWindow(primaryStage, controller);
            
            // Initialize the application (load settings, start monitoring, etc.)
            controller.initializeApplication();
            
            // Create and show the main window
            mainWindow.createAndShow();
            
            // Add shutdown hook to save settings
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Application shutting down via shutdown hook...");
                if (controller != null) {
                    controller.shutdown();
                }
            }));
            
            logger.info("Enhanced App Time Limiter started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start Enhanced App Time Limiter", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("JavaFX Application stop() called");
        if (controller != null) {
            controller.shutdown();
        }
        super.stop();
    }
}
