package com.apptimer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;

/**
 * Security manager for password protection
 */
public class SecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);
    
    // Default password hash (SHA-256 of "admin123")
    private String passwordHash = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
    private boolean isAuthenticated = false;
    private long authenticationTime = 0;
    private static final long AUTH_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    
    public SecurityManager() {
        logger.info("Security manager initialized");
    }
    
    /**
     * Show password dialog and authenticate user
     */
    public boolean authenticate(Stage parent, String action) {
        // Check if still authenticated within timeout
        if (isAuthenticated && (System.currentTimeMillis() - authenticationTime) < AUTH_TIMEOUT) {
            return true;
        }
        
        return showPasswordDialog(parent, action);
    }
    
    /**
     * Force authentication (ignores timeout)
     */
    public boolean forceAuthenticate(Stage parent, String action) {
        return showPasswordDialog(parent, action);
    }
    
    private boolean showPasswordDialog(Stage parent, String action) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parent);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Administrator Authentication Required");
        dialog.setResizable(false);
        
        // Make the dialog always on top and hard to close
        dialog.setAlwaysOnTop(true);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f0f0;");
        
        Label titleLabel = new Label("🔒 SECURITY CHECK");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");
        
        Label messageLabel = new Label("Administrator password required to: " + action);
        messageLabel.setStyle("-fx-font-size: 12px; -fx-wrap-text: true;");
        messageLabel.setMaxWidth(300);
        
        Label warningLabel = new Label("⚠️ Unauthorized access attempt will be logged");
        warningLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff6600;");
        
        // Error label for displaying authentication errors
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff0000; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setMaxWidth(300);
        errorLabel.setWrapText(true);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter administrator password");
        passwordField.setPrefWidth(250);
        
        Button okButton = new Button("Authenticate");
        Button cancelButton = new Button("Cancel");
        
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(10);
        buttonGrid.add(cancelButton, 0, 0);
        buttonGrid.add(okButton, 1, 0);
        
        final boolean[] result = {false};
        
        okButton.setOnAction(e -> {
            String password = passwordField.getText();
            if (validatePassword(password)) {
                isAuthenticated = true;
                authenticationTime = System.currentTimeMillis();
                result[0] = true;
                logger.info("Successful authentication for action: {}", action);
                dialog.close();
            } else {
                // Log failed attempt
                logger.warn("Failed authentication attempt for action: {} from user", action);
                
                // Show error in the same dialog
                errorLabel.setText("❌ INVALID PASSWORD\nIncorrect password. This attempt has been logged.");
                errorLabel.setVisible(true);
                
                // Clear password field and refocus
                passwordField.clear();
                passwordField.requestFocus();
                
                // Resize dialog to accommodate error message
                dialog.sizeToScene();
            }
        });
        
        cancelButton.setOnAction(e -> {
            logger.info("Authentication cancelled for action: {}", action);
            dialog.close();
        });
        
        // Prevent closing with X button
        dialog.setOnCloseRequest(e -> {
            e.consume(); // Prevent default close
            logger.warn("Attempted to close authentication dialog without proper authentication");
        });
        
        // Enter key submits
        passwordField.setOnAction(e -> okButton.fire());
        
        // Clear error when user starts typing again
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            if (errorLabel.isVisible()) {
                errorLabel.setVisible(false);
                dialog.sizeToScene();
            }
        });
        
        root.getChildren().addAll(
            titleLabel,
            messageLabel,
            warningLabel,
            errorLabel,
            new Label("Password:"),
            passwordField,
            buttonGrid
        );
        
        Scene scene = new Scene(root, 350, 280);
        dialog.setScene(scene);
        
        // Focus on password field
        passwordField.requestFocus();
        
        dialog.showAndWait();
        return result[0];
    }
    
    private boolean validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        String hash = hashPassword(password);
        return passwordHash.equals(hash);
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Error hashing password", e);
            return "";
        }
    }
    
    /**
     * Change the administrator password
     */
    public boolean changePassword(Stage parent) {
        if (!forceAuthenticate(parent, "change administrator password")) {
            return false;
        }
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parent);
        dialog.setTitle("Change Administrator Password");
        dialog.setResizable(false);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Change Administrator Password");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current password");
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password (min 6 characters)");
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        
        Button saveButton = new Button("Change Password");
        Button cancelButton = new Button("Cancel");
        
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(10);
        buttonGrid.add(cancelButton, 0, 0);
        buttonGrid.add(saveButton, 1, 0);
        
        final boolean[] result = {false};
        
        saveButton.setOnAction(e -> {
            String current = currentPasswordField.getText();
            String newPass = newPasswordField.getText();
            String confirm = confirmPasswordField.getText();
            
            if (!validatePassword(current)) {
                showAlert("Error", "Current password is incorrect");
                return;
            }
            
            if (newPass.length() < 6) {
                showAlert("Error", "New password must be at least 6 characters");
                return;
            }
            
            if (!newPass.equals(confirm)) {
                showAlert("Error", "New passwords do not match");
                return;
            }
            
            passwordHash = hashPassword(newPass);
            result[0] = true;
            logger.info("Administrator password changed successfully");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Password Changed");
            alert.setContentText("Administrator password has been changed successfully.");
            alert.showAndWait();
            
            dialog.close();
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        root.getChildren().addAll(
            titleLabel,
            new Label("Current Password:"), currentPasswordField,
            new Label("New Password:"), newPasswordField,
            new Label("Confirm Password:"), confirmPasswordField,
            buttonGrid
        );
        
        Scene scene = new Scene(root, 300, 280);
        dialog.setScene(scene);
        dialog.showAndWait();
        
        return result[0];
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Get current password hash for saving to config
     */
    public String getPasswordHash() {
        return passwordHash;
    }
    
    /**
     * Set password hash from config
     */
    public void setPasswordHash(String hash) {
        this.passwordHash = hash;
    }
    
    /**
     * Logout (clear authentication)
     */
    public void logout() {
        isAuthenticated = false;
        authenticationTime = 0;
        logger.info("User logged out");
    }
    
    /**
     * Check if user is currently authenticated
     */
    public boolean isAuthenticated() {
        if (!isAuthenticated) {
            return false;
        }
        
        // Check timeout
        if ((System.currentTimeMillis() - authenticationTime) >= AUTH_TIMEOUT) {
            logout();
            return false;
        }
        
        return true;
    }
}
