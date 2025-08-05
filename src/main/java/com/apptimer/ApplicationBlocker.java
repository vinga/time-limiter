package com.apptimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Enhanced Application Blocker with configurable delays and comprehensive blocking
 */
public class ApplicationBlocker {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationBlocker.class);
    
    private final Map<String, LocalDateTime> blockedUntil = new ConcurrentHashMap<>();
    private final Map<String, Integer> defaultBlockDelayMinutes = new ConcurrentHashMap<>();
    private final ProcessMonitor processMonitor;
    private final VoiceNotifier voiceNotifier;
    
    public ApplicationBlocker(ProcessMonitor processMonitor, VoiceNotifier voiceNotifier) {
        this.processMonitor = processMonitor;
        this.voiceNotifier = voiceNotifier;
        
        // Initialize default block delays (configurable per app)
        defaultBlockDelayMinutes.put("minecraft.exe", 60); // 1 hour default
        defaultBlockDelayMinutes.put("chrome.exe", 60);    // 1 hour default
    }
    
    /**
     * Set the default block delay for an application when time limit is exceeded
     */
    public void setDefaultBlockDelay(String processName, int minutes) {
        defaultBlockDelayMinutes.put(processName.toLowerCase(), minutes);
        logger.info("Default block delay for {} set to {} minutes", getAppDisplayName(processName), minutes);
    }
    
    /**
     * Get the default block delay for an application
     */
    public int getDefaultBlockDelay(String processName) {
        return defaultBlockDelayMinutes.getOrDefault(processName.toLowerCase(), 60);
    }
    
    /**
     * Block an application using its configured default delay when time limit exceeded
     */
    public void blockApplicationWithDefaultDelay(String processName, String reason) {
        int delayMinutes = getDefaultBlockDelay(processName);
        blockApplicationMinutes(processName, delayMinutes, reason);
    }
    
    /**
     * Block an application for a specified number of minutes
     */
    public void blockApplicationMinutes(String processName, int minutes, String reason) {
        LocalDateTime blockUntil = LocalDateTime.now().plusMinutes(minutes);
        blockedUntil.put(processName.toLowerCase(), blockUntil);
        
        String appName = getAppDisplayName(processName);
        logger.info("Application {} blocked until {} ({} minutes) - Reason: {}", 
                   appName, blockUntil.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")), minutes, reason);
        
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
     * Block Chrome using its configured default delay
     */
    public void blockChromeWithConfiguredDelay() {
        blockApplicationWithDefaultDelay("chrome.exe", "Time limit exceeded");
    }
    
    /**
     * Block Minecraft using its configured default delay
     */
    public void blockMinecraftWithConfiguredDelay() {
        blockApplicationWithDefaultDelay("minecraft.exe", "Time limit exceeded");
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
            voiceNotifier.announceBlockExpired(getAppDisplayName(processName));
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
     * Get remaining block time formatted as hours:minutes
     */
    public String getRemainingBlockTimeFormatted(String processName) {
        long totalMinutes = getRemainingBlockTime(processName);
        if (totalMinutes <= 0) {
            return "Not blocked";
        }
        
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d", hours, minutes);
        } else {
            return String.format("%d min", minutes);
        }
    }
    
    /**
     * Handle blocked application detection with enhanced protection
     */
    public void handleBlockedApplication(String processName) {
        String appName = getAppDisplayName(processName);
        long remainingMinutes = getRemainingBlockTime(processName);
        
        logger.warn("BLOCKED APPLICATION DETECTED: {} attempting to run. {} minutes remaining in block.", 
                   appName, remainingMinutes);
        
        // Immediate aggressive termination
        terminateProcessAggressively(processName);
        
        // Voice warning with remaining time
        voiceNotifier.sayBlockedAttempt(appName, remainingMinutes);
        
        // Log security event
        logger.warn("SECURITY EVENT: Blocked application {} was terminated. Block remaining: {} minutes", 
                   appName, remainingMinutes);
    }
    
    /**
     * Unblock an application (admin function)
     */
    public void unblockApplication(String processName) {
        blockedUntil.remove(processName.toLowerCase());
        String appName = getAppDisplayName(processName);
        logger.info("Application {} unblocked by administrator", appName);
        voiceNotifier.announceUnblocked(appName);
    }
    
    /**
     * Get all currently blocked applications with their remaining time
     */
    public Map<String, Long> getBlockedApplications() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up expired blocks
        blockedUntil.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        
        for (Map.Entry<String, LocalDateTime> entry : blockedUntil.entrySet()) {
            long minutes = java.time.Duration.between(now, entry.getValue()).toMinutes();
            if (minutes > 0) {
                result.put(entry.getKey(), minutes);
            }
        }
        
        return result;
    }
    
    /**
     * Check if any applications are currently blocked
     */
    public boolean hasBlockedApplications() {
        return !getBlockedApplications().isEmpty();
    }
    
    private void terminateIfRunning(String processName) {
        // Check if process is currently running and terminate it
        try {
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq " + processName);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // Process found, terminate it aggressively
                terminateProcessAggressively(processName);
            }
        } catch (Exception e) {
            logger.error("Error checking if process {} is running", processName, e);
        }
    }
    
    private void terminateProcessAggressively(String processName) {
        try {
            // First attempt: Force terminate
            ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", processName);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("Successfully terminated blocked process {}", processName);
            } else {
                logger.warn("Standard termination failed for {} (exit code: {}), attempting enhanced termination", processName, exitCode);
                
                // Second attempt: Kill all instances recursively
                ProcessBuilder pb2 = new ProcessBuilder("taskkill", "/F", "/T", "/IM", processName);
                Process process2 = pb2.start();
                int exitCode2 = process2.waitFor();
                
                if (exitCode2 == 0) {
                    logger.info("Successfully terminated blocked process {} with enhanced method", processName);
                } else {
                    logger.error("Failed to terminate blocked process {} with all methods (exit code: {})", processName, exitCode2);
                }
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
    
    /**
     * Get all block delays for configuration persistence
     */
    public Map<String, Integer> getAllBlockDelays() {
        return new ConcurrentHashMap<>(defaultBlockDelayMinutes);
    }
    
    /**
     * Set all block delays from configuration
     */
    public void setAllBlockDelays(Map<String, Integer> delays) {
        defaultBlockDelayMinutes.clear();
        defaultBlockDelayMinutes.putAll(delays);
        logger.info("Block delays loaded from configuration");
    }
}
