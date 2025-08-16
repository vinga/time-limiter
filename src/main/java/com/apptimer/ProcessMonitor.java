package com.apptimer;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced process monitor with better blocking integration
 */
public class ProcessMonitor {
    private static final Logger logger = LoggerFactory.getLogger(ProcessMonitor.class);
    
    private ScheduledExecutorService scheduler;
    private boolean isMonitoring = false;
    private final Set<String> targetProcesses;
    private ApplicationBlocker applicationBlocker;
    
    public ProcessMonitor() {
        targetProcesses = new HashSet<>();
        targetProcesses.add("Minecraft.exe");
        targetProcesses.add("chrome.exe");
    }
    
    public void startMonitoring(TimeTracker timeTracker, VoiceNotifier voiceNotifier) {
        if (isMonitoring) {
            logger.warn("Monitoring already started");
            return;
        }
        
        // Initialize application blocker
        if (applicationBlocker == null) {
            applicationBlocker = new ApplicationBlocker(this, voiceNotifier);
        }
        
        isMonitoring = true;
        scheduler = Executors.newScheduledThreadPool(1);
        
        // Check processes every 2 seconds for faster response
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkRunningProcesses(timeTracker, voiceNotifier);
            } catch (Exception e) {
                logger.error("Error during process monitoring", e);
            }
        }, 0, 2, TimeUnit.SECONDS);
        
        // Announce monitoring started
        voiceNotifier.announceMonitoringStarted();
        
        logger.info("Process monitoring started with enhanced blocking");
    }
    
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        isMonitoring = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Process monitoring stopped");
    }
    
    private void checkRunningProcesses(TimeTracker timeTracker, VoiceNotifier voiceNotifier) {
        Set<String> runningProcesses = getRunningProcesses();
        
        for (String process : targetProcesses) {
            if (runningProcesses.contains(process)) {
                // FIRST: Check if application is blocked - this takes priority
                if (applicationBlocker.isBlocked(process)) {
                    applicationBlocker.handleBlockedApplication(process);
                    continue; // Skip all other processing for blocked apps
                }
                
                // Record normal activity for non-blocked apps
                timeTracker.recordActivity(process);
                
                // Check if warning should be issued
                if (timeTracker.shouldWarn(process)) {
                    String appName = getAppDisplayName(process);
                    voiceNotifier.sayWarning(appName, timeTracker.getRemainingTime(process));
                }
                
                // Check if time limit exceeded
                if (timeTracker.isTimeExceeded(process)) {
                    handleTimeLimitExceeded(process, timeTracker, voiceNotifier);
                }
            }
        }
    }
    
    /**
     * Handle when a time limit is exceeded - enhanced with configurable blocking
     */
    private void handleTimeLimitExceeded(String process, TimeTracker timeTracker, VoiceNotifier voiceNotifier) {
        logger.info("Time limit exceeded for {}, implementing enhanced blocking...", process);
        
        String appName = getAppDisplayName(process);
        
        // STEP 1: Block the application using configured delay
        if (process.equalsIgnoreCase("chrome.exe")) {
            applicationBlocker.blockChromeWithConfiguredDelay();
        } else if (process.equalsIgnoreCase("minecraft.exe")) {
            applicationBlocker.blockMinecraftWithConfiguredDelay();
        } else {
            // For other apps, use default block with configured delay
            applicationBlocker.blockApplicationWithDefaultDelay(process, "Time limit exceeded");
        }
        
        // STEP 2: Reset the time tracker to prevent re-triggering
        timeTracker.resetTime(process);
        
        // STEP 3: Terminate the currently running process
        terminateProcess(process);
        
        // STEP 4: Voice notification
        voiceNotifier.sayTimeUp(appName);
        
        logger.info("{} blocked for {} minutes due to time limit exceeded", 
                   appName, applicationBlocker.getDefaultBlockDelay(process));
    }
    
    private Set<String> getRunningProcesses() {
        Set<String> processes = new HashSet<>();
        
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
            Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));
        
        if (snapshot == null) {
            logger.error("Failed to create process snapshot");
            return processes;
        }
        
        try {
            Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
            
            if (Kernel32.INSTANCE.Process32First(snapshot, processEntry)) {
                do {
                    String processName = new String(processEntry.szExeFile).trim();
                    if (processName.contains("\0")) {
                        processName = processName.substring(0, processName.indexOf('\0'));
                    }
                    processes.add(processName.toLowerCase());
                } while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry));
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }
        
        return processes;
    }
    
    private void terminateProcess(String processName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", processName);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("Successfully terminated {}", processName);
            } else {
                logger.warn("Failed to terminate {} (exit code: {})", processName, exitCode);
            }
        } catch (Exception e) {
            logger.error("Error terminating process {}", processName, e);
        }
    }
    
    private String getAppDisplayName(String processName) {
        switch (processName.toLowerCase()) {
            case "minecraft.exe":
                return "Minecraft";
            case "chrome.exe":
                return "Chrome";
            default:
                return processName;
        }
    }
    
    public boolean isMonitoring() {
        return isMonitoring;
    }
    
    public ApplicationBlocker getApplicationBlocker() {
        return applicationBlocker;
    }
    
    /**
     * Add a new process to monitor
     */
    public void addTargetProcess(String processName) {
        targetProcesses.add(processName.toLowerCase());
        logger.info("Added {} to monitoring targets", processName);
    }
    
    /**
     * Remove a process from monitoring
     */
    public void removeTargetProcess(String processName) {
        targetProcesses.remove(processName.toLowerCase());
        logger.info("Removed {} from monitoring targets", processName);
    }
    
    /**
     * Get all target processes
     */
    public Set<String> getTargetProcesses() {
        return new HashSet<>(targetProcesses);
    }
}
