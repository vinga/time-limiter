package com.apptimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manages application blocking functionality
 */
public class ApplicationBlocker {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationBlocker.class);
    
    private final Map<String, LocalDateTime> blockedUntil = new ConcurrentHashMap<>();
    private final ProcessMonitor processMonitor;
    private final VoiceNotifier voiceNotifier;
    
    public ApplicationBlocker(ProcessMonitor processMonitor, VoiceNotifier voiceNotifier) {
        this.processMonitor = processMonitor;
        this.voiceNotifier = voiceNotifier;
    }
    
    /**
     * Block an application for a specified number of minutes
     */
    public void blockApplicationMinutes(String processName, int minutes, String reason) {
        LocalDateTime blockUntil = LocalDateTime.now().plusMinutes(minutes);
        blockedUntil.put(processName.toLowerCase(), blockUntil);
        
        String appName = getAppDisplayName(processName);
        logger.info("Application {} blocked until {} - Reason: {}", 
                   appName, blockUntil.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")), reason);
        
        // Immediately terminate if running
        terminateIfRunning(processName);
        
        // Voice notification with minutes
        voiceNotifier.announceBlockMinutes(appName, minutes, reason);
    }
    
    /**
     * Block an application for a specified number of hours
     */
    public void blockApplication(String processName, int hours, String reason) {
        LocalDateTime blockUntil = LocalDateTime.now().plusHours(hours);
        blockedUntil.put(processName.toLowerCase(), blockUntil);
        
        String appName = getAppDisplayName(processName);
        logger.info("Application {} blocked until {} - Reason: {}", 
                   appName, blockUntil.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")), reason);
        
        // Immediately terminate if running
        terminateIfRunning(processName);
        
        // Voice notification
        voiceNotifier.announceBlock(appName, hours, reason);
    }
    
    /**
     * Block Chrome for 3 hours due to time limit exceeded
     */
    public void blockChromeFor3Hours() {
        blockApplication("chrome.exe", 3, "Time limit exceeded");
    }
    
    /**
     * Check if an application is currently blocked
     */
    public boolean isBlocked(String processName) {
        LocalDateTime blockTime = blockedUntil.get(processName.toLowerCase());
        if (blockTime == null) {
            return false;
        }
        
        // Check if block has expired
        if (LocalDateTime.now().isAfter(blockTime)) {
            blockedUntil.remove(processName.toLowerCase());
            logger.info("Block expired for {}", getAppDisplayName(processName));
            return false;
        }
        
        logger.debug("{} is blocked until {}", getAppDisplayName(processName), 
                    blockTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return true;
    }
    
    /**
     * Get remaining block time in minutes
     */
    public long getRemainingBlockTime(String processName) {
        LocalDateTime blockTime = blockedUntil.get(processName.toLowerCase());
        if (blockTime == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(blockTime)) {
            return 0;
        }
        
        return java.time.Duration.between(now, blockTime).toMinutes();
    }
    
    /**
     * Handle blocked application detection
     */
    public void handleBlockedApplication(String processName) {
        String appName = getAppDisplayName(processName);
        long remainingMinutes = getRemainingBlockTime(processName);
        
        logger.warn("Blocked application {} detected, terminating. {} minutes remaining.", 
                   appName, remainingMinutes);
        
        // Terminate the process
        terminateProcess(processName);
        
        // Voice warning
        voiceNotifier.sayBlockedAttempt(appName, remainingMinutes);
    }
    
    /**
     * Unblock an application (admin function)
     */
    public void unblockApplication(String processName) {
        blockedUntil.remove(processName.toLowerCase());
        String appName = getAppDisplayName(processName);
        logger.info("Application {} unblocked by administrator", appName);
    }
    
    /**
     * Get all currently blocked applications with their remaining time
     */
    public Map<String, Long> getBlockedApplications() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        blockedUntil.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        
        for (Map.Entry<String, LocalDateTime> entry : blockedUntil.entrySet()) {
            long minutes = java.time.Duration.between(now, entry.getValue()).toMinutes();
            if (minutes > 0) {
                result.put(entry.getKey(), minutes);
            }
        }
        
        return result;
    }
    
    private void terminateIfRunning(String processName) {
        // Check if process is currently running and terminate it
        try {
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq " + processName);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // Process found, terminate it
                terminateProcess(processName);
            }
        } catch (Exception e) {
            logger.error("Error checking if process {} is running", processName, e);
        }
    }
    
    private void terminateProcess(String processName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", processName);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("Successfully terminated blocked process {}", processName);
            } else {
                logger.warn("Failed to terminate blocked process {} (exit code: {})", processName, exitCode);
            }
        } catch (Exception e) {
            logger.error("Error terminating blocked process {}", processName, e);
        }
    }
    
    private String getAppDisplayName(String processName) {
        switch (processName.toLowerCase()) {
            case "chrome.exe":
                return "Google Chrome";
            case "minecraft.exe":
                return "Minecraft";
            case "firefox.exe":
                return "Firefox";
            case "msedge.exe":
                return "Microsoft Edge";
            default:
                return processName;
        }
    }
    
    /**
     * Get formatted string of blocked applications for display
     */
    public String getBlockedApplicationsStatus() {
        Map<String, Long> blocked = getBlockedApplications();
        if (blocked.isEmpty()) {
            return "No applications currently blocked";
        }
        
        StringBuilder status = new StringBuilder("Blocked Applications:\n");
        for (Map.Entry<String, Long> entry : blocked.entrySet()) {
            String appName = getAppDisplayName(entry.getKey());
            long minutes = entry.getValue();
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            
            status.append(String.format("• %s: %d:%02d remaining\n", 
                         appName, hours, remainingMinutes));
        }
        
        return status.toString();
    }
}
