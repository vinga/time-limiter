package com.apptimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Tracks time spent on applications and manages time limits
 */
public class TimeTracker {
    private static final Logger logger = LoggerFactory.getLogger(TimeTracker.class);
    
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> totalTime = new ConcurrentHashMap<>();
    private final Map<String, Long> timeLimits = new ConcurrentHashMap<>();
    private final Map<String, Boolean> warningIssued = new ConcurrentHashMap<>();
    
    private long warningTimeSeconds = 5 * 60; // 5 minutes default
    
    public void recordActivity(String processName) {
        long currentTime = System.currentTimeMillis();
        
        if (!startTimes.containsKey(processName)) {
            // First time seeing this process
            startTimes.put(processName, currentTime);
            logger.debug("Started tracking {}", processName);
        } else {
            // Update total time
            long startTime = startTimes.get(processName);
            long sessionTime = currentTime - startTime;
            totalTime.merge(processName, sessionTime, Long::sum);
            startTimes.put(processName, currentTime); // Reset start time
        }
    }
    
    public void setTimeLimit(String processName, long limitInSeconds) {
        timeLimits.put(processName, limitInSeconds * 1000L); // Convert to milliseconds
        logger.info("Set time limit for {} to {} seconds", processName, limitInSeconds);
    }
    
    public void setWarningTime(long warningTimeInSeconds) {
        this.warningTimeSeconds = warningTimeInSeconds;
        logger.info("Set warning time to {} seconds", warningTimeInSeconds);
    }
    
    public boolean shouldWarn(String processName) {
        if (!timeLimits.containsKey(processName)) {
            return false;
        }
        
        if (warningIssued.getOrDefault(processName, false)) {
            return false; // Already warned for this session
        }
        
        long totalTimeMs = totalTime.getOrDefault(processName, 0L);
        long limitMs = timeLimits.get(processName);
        long warningThresholdMs = limitMs - (warningTimeSeconds * 1000L);
        
        if (totalTimeMs >= warningThresholdMs) {
            warningIssued.put(processName, true);
            return true;
        }
        
        return false;
    }
    
    public boolean isTimeExceeded(String processName) {
        if (!timeLimits.containsKey(processName)) {
            return false;
        }
        
        long totalTimeMs = totalTime.getOrDefault(processName, 0L);
        long limitMs = timeLimits.get(processName);
        
        return totalTimeMs >= limitMs;
    }
    
    public long getRemainingTime(String processName) {
        if (!timeLimits.containsKey(processName)) {
            return 0;
        }
        
        long totalTimeMs = totalTime.getOrDefault(processName, 0L);
        long limitMs = timeLimits.get(processName);
        long remainingMs = limitMs - totalTimeMs;
        
        return Math.max(0, remainingMs / 1000L); // Convert to seconds
    }
    
    public long getTotalTime(String processName) {
        return totalTime.getOrDefault(processName, 0L) / 1000L; // Convert to seconds
    }
    
    public void resetTime(String processName) {
        totalTime.remove(processName);
        startTimes.remove(processName);
        warningIssued.remove(processName);
        logger.info("Reset time tracking for {}", processName);
    }
    
    public void resetAllTimes() {
        totalTime.clear();
        startTimes.clear();
        warningIssued.clear();
        logger.info("Reset all time tracking");
    }
    
    public void setTotalTime(String processName, long totalTimeSeconds) {
        totalTime.put(processName, totalTimeSeconds * 1000L); // Convert to milliseconds
        logger.info("Set total time for {} to {} seconds", processName, totalTimeSeconds);
    }
    
    public Map<String, Long> getAllTotalTimes() {
        Map<String, Long> times = new ConcurrentHashMap<>();
        totalTime.forEach((process, timeMs) -> times.put(process, timeMs / 1000L));
        return times;
    }
    
    public void printStatus() {
        logger.info("=== Time Tracking Status ===");
        for (Map.Entry<String, Long> entry : totalTime.entrySet()) {
            String process = entry.getKey();
            long timeSeconds = entry.getValue() / 1000L;
            long limitSeconds = timeLimits.getOrDefault(process, 0L) / 1000L;
            
            logger.info("{}: {}s / {}s ({}%)", 
                process, timeSeconds, limitSeconds, 
                limitSeconds > 0 ? (timeSeconds * 100 / limitSeconds) : 0);
        }
        logger.info("===========================");
    }
}
