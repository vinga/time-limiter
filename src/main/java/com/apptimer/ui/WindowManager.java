package com.apptimer.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apptimer.controller.MainApplicationController;
import java.awt.TrayIcon;

/**
 * Manages window operations including creation, protection, and visibility
 */
public class WindowManager {
    private static final Logger logger = LoggerFactory.getLogger(WindowManager.class);
    
    private final Stage primaryStage;
    private final MainApplicationController controller;
    
    public WindowManager(Stage primaryStage, MainApplicationController controller) {
        this.primaryStage = primaryStage;
        this.controller = controller;
    }
    
    /**
     * Create the main application window with all sections
     */
    public void createMainWindow(VBox leftSide, VBox rightSide, MenuBarManager menuManager) {
        primaryStage.setTitle("🔒 Enhanced App Time Limiter - Kid-Safe Mode");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        
        BorderPane root = new BorderPane();
        
        // Configure left side
        leftSide.setPadding(new Insets(15));
        leftSide.setPrefWidth(580);
        
        // Configure right side  
        rightSide.setPadding(new Insets(15));
        rightSide.setPrefWidth(600);
        
        // Add menu bar to left side as first element
        leftSide.getChildren().add(0, menuManager.createMenuBar());
        
        // Add scroll functionality to left side
        javafx.scene.control.ScrollPane leftScrollPane = new javafx.scene.control.ScrollPane(leftSide);
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScrollPane.setStyle("-fx-background-color: transparent;");
        
        // Set up the layout
        root.setLeft(leftScrollPane);
        root.setRight(rightSide);
        
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        
        // Setup window protection
        setupWindowProtection(scene);
        
        // Setup window close behavior
        setupCloseProtection();
        
        // Show window and then minimize to tray
        showAndMinimizeToTray();
        
        logger.info("Main window created successfully");
    }
    
    /**
     * Setup keyboard protection to block shortcuts
     */
    private void setupWindowProtection(Scene scene) {
        scene.setOnKeyPressed(e -> {
            // Block Alt+F4 (common close shortcut)
            if (e.getCode() == KeyCode.F4 && e.isAltDown()) {
                e.consume();
                logger.info("Alt+F4 blocked (enhanced protection)");
                showProtectionMessage("Alt+F4 is disabled in enhanced security mode");
            }
            // Block Ctrl+Q (common quit shortcut)
            else if (e.getCode() == KeyCode.Q && e.isControlDown()) {
                e.consume();
                logger.info("Ctrl+Q blocked (enhanced protection)");
                showProtectionMessage("Ctrl+Q is disabled in enhanced security mode");
            }
            // Block Ctrl+W (close window)
            else if (e.getCode() == KeyCode.W && e.isControlDown()) {
                e.consume();
                logger.info("Ctrl+W blocked (enhanced protection)");
                showProtectionMessage("Ctrl+W is disabled in enhanced security mode");
            }
        });
        
        logger.info("Enhanced keyboard protection enabled");
    }
    
    /**
     * Setup close protection to minimize instead of close
     */
    private void setupCloseProtection() {
        primaryStage.setOnCloseRequest(e -> {
            e.consume(); // Prevent default close
            logger.info("Close request intercepted - minimizing to system tray (enhanced protection)");
            
            minimizeToTray();
            showProtectionMessage("App minimized to system tray for enhanced protection");
        });
        
        logger.info("Enhanced close protection enabled");
    }
    
    /**
     * Show window initially and then minimize to tray for kid-safe mode
     */
    private void showAndMinimizeToTray() {
        // Show window initially for full initialization
        primaryStage.show();
        
        // Enable protection immediately
        controller.setProtectionActive(true);
        
        // Auto-minimize to tray after brief initialization delay
        Platform.runLater(() -> Platform.runLater(() -> {
            logger.info("Auto-minimizing to system tray for enhanced kid-safe mode");
            minimizeToTray();
            
            showTrayMessage("🔒 Enhanced App Time Limiter Started", 
                          "Running in enhanced kid-safe mode. Right-click tray icon to access.",
                          TrayIcon.MessageType.INFO);
        }));
    }
    
    /**
     * Show window from system tray
     */
    public void showWindow() {
        if (primaryStage != null) {
            logger.info("Showing window from tray (enhanced restore)");
            
            try {
                // Restore from minimized state
                if (primaryStage.isIconified()) {
                    primaryStage.setIconified(false);
                }
                
                // Ensure window is visible and focused
                primaryStage.show();
                primaryStage.toFront();
                primaryStage.requestFocus();
                
                logger.info("Window restored successfully (enhanced mode)");
                
            } catch (Exception e) {
                logger.error("Error showing window in enhanced mode", e);
            }
        } else {
            logger.error("primaryStage is null in enhanced mode!");
        }
    }
    
    /**
     * Hide window to system tray
     */
    public void hideWindow() {
        minimizeToTray();
    }
    
    /**
     * Minimize window to system tray
     */
    private void minimizeToTray() {
        if (primaryStage != null) {
            logger.info("Minimizing window to tray (enhanced protection)");
            
            try {
                primaryStage.setIconified(true);
                logger.info("Window minimized successfully (enhanced mode)");
                
            } catch (Exception e) {
                logger.error("Error minimizing window in enhanced mode", e);
            }
        } else {
            logger.error("primaryStage is null during minimize in enhanced mode!");
        }
    }
    
    /**
     * Toggle window visibility
     */
    public void toggleWindowVisibility() {
        if (primaryStage != null) {
            if (primaryStage.isShowing() && !primaryStage.isIconified()) {
                logger.info("Window is showing, hiding it (enhanced mode)");
                hideWindow();
            } else {
                logger.info("Window is hidden or minimized, showing it (enhanced mode)");
                showWindow();
            }
        }
    }
    
    /**
     * Show protection message via system tray
     */
    private void showProtectionMessage(String message) {
        showTrayMessage("🛡️ Enhanced Protection Active", message, TrayIcon.MessageType.WARNING);
    }
    
    /**
     * Show tray message if tray is available
     */
    private void showTrayMessage(String title, String message, TrayIcon.MessageType type) {
        if (controller.getTrayManager() != null && controller.getTrayManager().getTrayIcon() != null) {
            controller.getTrayManager().getTrayIcon().displayMessage(title, message, type);
        }
    }
    
    /**
     * Get the primary stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
