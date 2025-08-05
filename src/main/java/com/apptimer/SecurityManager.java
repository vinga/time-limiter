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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced security manager with additional protection features
 */
public class SecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);
    
    // Default password hash (SHA-256 of "admin123")
    private String passwordHash = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
    private boolean isAuthenticated = false;
    private long authenticationTime = 0;
    private static final long AUTH_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    private int failedAttempts = 0;
    private long lockoutUntil = 0;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 5 * 60 * 1000; // 5 minutes
    
    // Enhanced security features
    private VoiceNotifier voiceNotifier;
    
    public SecurityManager() {
        logger.info("Enhanced security manager initialized");
    }
    
    /**
     * Set the voice notifier for security announcements
     */
    public void setVoiceNotifier(VoiceNotifier voiceNotifier) {
        this.voiceNotifier = voiceNotifier;
    }
    
    /**
     * Show password dialog and authenticate user with enhanced security
     */
    public boolean authenticate(Stage parent, String action) {
        // Check if still authenticated within timeout
        if (isAuthenticated && (System.currentTimeMillis() - authenticationTime) < AUTH_TIMEOUT) {
            logger.debug("Using cached authentication for action: {}", action);
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
    
    /**
     * Check if currently in security lockout
     */
    public boolean isInLockout() {
        if (lockoutUntil > 0 && System.currentTimeMillis() < lockoutUntil) {
            return true;
        }
        
        // Clear lockout if expired
        if (lockoutUntil > 0 && System.currentTimeMillis() >= lockoutUntil) {
            lockoutUntil = 0;
            failedAttempts = 0;
            logger.info("Security lockout expired, access restored");
        }
        
        return false;
    }
    
    /**
     * Get remaining lockout time in minutes
     */
    public long getRemainingLockoutMinutes() {
        if (!isInLockout()) {
            return 0;
        }
        
        return (lockoutUntil - System.currentTimeMillis()) / (60 * 1000);
    }
    
    private boolean showPasswordDialog(Stage parent, String action) {
        // Check for security lockout
        if (isInLockout()) {
            long remainingMinutes = getRemainingLockoutMinutes();
            String lockoutMessage = String.format(
                "🔒 SECURITY LOCKOUT ACTIVE\n\n" +
                "Too many failed authentication attempts.\n" +
                "Access will be restored in %d minutes.\n\n" +
                "All attempts are being logged.", remainingMinutes);
            
            Alert lockoutAlert = new Alert(Alert.AlertType.ERROR);
            lockoutAlert.setTitle("Security Lockout");
            lockoutAlert.setHeaderText("Access Temporarily Denied");
            lockoutAlert.setContentText(lockoutMessage);
            lockoutAlert.showAndWait();
            
            // Voice notification for security violation
            if (voiceNotifier != null) {
                voiceNotifier.announceSecurityViolation("access system during lockout");
            }
            
            logger.warn("SECURITY: Authentication attempt during lockout for action: {}", action);
            return false;
        }
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parent);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("🔒 Administrator Authentication Required");
        dialog.setResizable(false);
        dialog.setAlwaysOnTop(true);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f0f0;");
        
        Label titleLabel = new Label("🔒 SECURITY CHECK");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");
        
        Label messageLabel = new Label("Administrator password required to: " + action);
        messageLabel.setStyle("-fx-font-size: 12px; -fx-wrap-text: true;");
        messageLabel.setMaxWidth(350);
        
        Label warningLabel = new Label("⚠️ Unauthorized access attempts will be logged and may result in lockout");
        warningLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff6600;");
        warningLabel.setWrapText(true);
        warningLabel.setMaxWidth(350);
        
        // Show failed attempts if any
        Label attemptsLabel = new Label("");
        if (failedAttempts > 0) {
            attemptsLabel.setText(String.format("⚠️ Failed attempts: %d/%d", failedAttempts, MAX_FAILED_ATTEMPTS));
            attemptsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ff0000; -fx-font-weight: bold;");
        }
        
        // Error label for displaying authentication errors
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff0000; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setMaxWidth(350);
        errorLabel.setWrapText(true);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter administrator password");
        passwordField.setPrefWidth(300);
        
        Button okButton = new Button("Authenticate");
        Button cancelButton = new Button("Cancel");
        
        okButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold;");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(10);
        buttonGrid.add(cancelButton, 0, 0);
        buttonGrid.add(okButton, 1, 0);
        
        final boolean[] result = {false};
        
        okButton.setOnAction(e -> {
            String password = passwordField.getText();
            if (validatePassword(password)) {
                // Successful authentication
                isAuthenticated = true;
                authenticationTime = System.currentTimeMillis();
                result[0] = true;
                
                // Reset failed attempts on success
                failedAttempts = 0;
                lockoutUntil = 0;
                
                logger.info("SECURITY: Successful authentication for action: {}", action);
                dialog.close();
            } else {
                // Failed authentication
                failedAttempts++;
                
                logger.warn("SECURITY: Failed authentication attempt #{} for action: {} at {}", 
                           failedAttempts, action, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
                
                // Check if lockout should be triggered
                if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                    lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION;
                    
                    logger.error("SECURITY ALERT: Maximum failed attempts reached. System locked until {}", 
                               LocalDateTime.now().plusMinutes(5).format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
                    
                    // Voice security alert
                    if (voiceNotifier != null) {
                        voiceNotifier.announceSecurityViolation("exceed maximum authentication attempts");
                    }
                    
                    errorLabel.setText("🚨 SECURITY LOCKOUT ACTIVATED\nMaximum attempts exceeded.\nSystem locked for 5 minutes.");
                    errorLabel.setVisible(true);
                    
                    // Disable authentication controls
                    passwordField.setDisable(true);
                    okButton.setDisable(true);
                    
                } else {
                    int remainingAttempts = MAX_FAILED_ATTEMPTS - failedAttempts;
                    errorLabel.setText(String.format("❌ INVALID PASSWORD\nIncorrect password. %d attempts remaining.\nThis attempt has been logged.", remainingAttempts));
                    errorLabel.setVisible(true);
                    
                    // Update attempts label
                    attemptsLabel.setText(String.format("⚠️ Failed attempts: %d/%d", failedAttempts, MAX_FAILED_ATTEMPTS));
                    attemptsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ff0000; -fx-font-weight: bold;");
                }
                
                // Clear password field and refocus
                passwordField.clear();
                passwordField.requestFocus();
                
                // Resize dialog to accommodate error message
                dialog.sizeToScene();
            }
        });
        
        cancelButton.setOnAction(e -> {
            logger.info("SECURITY: Authentication cancelled for action: {}", action);
            dialog.close();
        });
        
        // Enhanced protection: Prevent closing with X button during authentication
        dialog.setOnCloseRequest(e -> {
            e.consume(); // Prevent default close
            logger.warn("SECURITY: Attempted to close authentication dialog without proper authentication for action: {}", action);
            
            if (voiceNotifier != null) {
                voiceNotifier.announceSecurityViolation("bypass authentication dialog");
            }
        });
        
        // Enter key submits
        passwordField.setOnAction(e -> okButton.fire());
        
        // Clear error when user starts typing again
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            if (errorLabel.isVisible() && !passwordField.isDisabled()) {
                errorLabel.setVisible(false);
                dialog.sizeToScene();
            }
        });
        
        root.getChildren().addAll(
            titleLabel,
            messageLabel,
            warningLabel,
            attemptsLabel,
            errorLabel,
            new Label("Password:"),
            passwordField,
            buttonGrid
        );
        
        Scene scene = new Scene(root, 400, 350);
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
     * Change the administrator password with enhanced security
     */
    public boolean changePassword(Stage parent) {
        if (!forceAuthenticate(parent, "change administrator password")) {
            return false;
        }
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parent);
        dialog.setTitle("🔒 Change Administrator Password");
        dialog.setResizable(false);
        dialog.setAlwaysOnTop(true);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        
        Label titleLabel = new Label("🔒 Change Administrator Password");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");
        
        Label securityLabel = new Label("⚠️ Choose a strong password with at least 8 characters");
        securityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ff6600;");
        securityLabel.setWrapText(true);
        
        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current password");
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password (min 8 characters)");
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        
        // Password strength indicator
        Label strengthLabel = new Label("");
        strengthLabel.setStyle("-fx-font-size: 10px;");
        
        newPasswordField.textProperty().addListener((obs, oldText, newText) -> {
            updatePasswordStrength(newText, strengthLabel);
        });
        
        Button saveButton = new Button("Change Password");
        Button cancelButton = new Button("Cancel");
        
        saveButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold;");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        
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
            
            if (newPass.length() < 8) {
                showAlert("Error", "New password must be at least 8 characters long");
                return;
            }
            
            if (!newPass.equals(confirm)) {
                showAlert("Error", "New passwords do not match");
                return;
            }
            
            if (newPass.equals(current)) {
                showAlert("Error", "New password must be different from current password");
                return;
            }
            
            passwordHash = hashPassword(newPass);
            result[0] = true;
            
            logger.info("SECURITY: Administrator password changed successfully at {}", 
                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Password Changed Successfully");
            alert.setContentText("Administrator password has been changed successfully.\n\nIMPORTANT: Remember your new password - it cannot be recovered!");
            alert.showAndWait();
            
            dialog.close();
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        root.getChildren().addAll(
            titleLabel,
            securityLabel,
            new Label("Current Password:"), currentPasswordField,
            new Label("New Password:"), newPasswordField, strengthLabel,
            new Label("Confirm Password:"), confirmPasswordField,
            buttonGrid
        );
        
        Scene scene = new Scene(root, 350, 380);
        dialog.setScene(scene);
        dialog.showAndWait();
        
        return result[0];
    }
    
    private void updatePasswordStrength(String password, Label strengthLabel) {
        if (password.length() < 8) {
            strengthLabel.setText("Password too short");
            strengthLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff0000;");
        } else if (password.length() < 12) {
            strengthLabel.setText("Password strength: Moderate");
            strengthLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff9800;");
        } else {
            strengthLabel.setText("Password strength: Strong");
            strengthLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #4caf50;");
        }
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
        logger.info("Password hash loaded from configuration");
    }
    
    /**
     * Logout (clear authentication)
     */
    public void logout() {
        isAuthenticated = false;
        authenticationTime = 0;
        logger.info("User logged out manually");
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
            logger.info("Authentication expired due to timeout");
            return false;
        }
        
        return true;
    }
    
    /**
     * Get security status summary
     */
    public String getSecurityStatus() {
        StringBuilder status = new StringBuilder();
        
        if (isInLockout()) {
            status.append(String.format("🔒 SECURITY LOCKOUT: %d minutes remaining\n", getRemainingLockoutMinutes()));
        } else if (failedAttempts > 0) {
            status.append(String.format("⚠️ Failed attempts: %d/%d\n", failedAttempts, MAX_FAILED_ATTEMPTS));
        } else {
            status.append("✅ Security status: Normal\n");
        }
        
        if (isAuthenticated()) {
            long remainingMinutes = (AUTH_TIMEOUT - (System.currentTimeMillis() - authenticationTime)) / (60 * 1000);
            status.append(String.format("Authenticated: %d minutes remaining\n", remainingMinutes));
        } else {
            status.append("🔒 Not authenticated\n");
        }
        
        return status.toString();
    }
    
    /**
     * Reset security lockout (admin emergency function)
     */
    public void resetSecurityLockout() {
        failedAttempts = 0;
        lockoutUntil = 0;
        logger.warn("SECURITY: Security lockout manually reset");
    }
}
