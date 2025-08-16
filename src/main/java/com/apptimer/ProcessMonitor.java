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
    private Set<String> previouslyRunningProcesses = new HashSet<>();
    private Runnable statusUpdateCallback;
    
    public ProcessMonitor() {
        targetProcesses = new HashSet<>();
        targetProcesses.add("minecraft.exe");
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
        boolean statusChanged = false;
        
        // Check for processes that stopped running
        for (String process : targetProcesses) {
            boolean wasRunning = previouslyRunningProcesses.contains(process);
            boolean isRunning = runningProcesses.contains(process);
            
            if (wasRunning && !isRunning) {
                // Process stopped running
                logger.debug("{} stopped running", getAppDisplayName(process));
                statusChanged = true;
            } else if (!wasRunning && isRunning) {
                // Process started running
                logger.debug("{} started running", getAppDisplayName(process));
                statusChanged = true;
            }
        }
        
        // Process currently running apps
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
        
        // Update previous state
        previouslyRunningProcesses = new HashSet<>(runningProcesses);
        previouslyRunningProcesses.retainAll(targetProcesses); // Only keep target processes
        
        // Trigger immediate status update if processes changed
        if (statusChanged && statusUpdateCallback != null) {
            statusUpdateCallback.run();
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
                    
                    // Special case: Check if javaw.exe is running Minecraft
                    if (processName.toLowerCase().equals("javaw.exe")) {
                        if (isMinecraftJavaProcess(processEntry.th32ProcessID.intValue())) {
                            processes.add("minecraft.exe"); // Treat Minecraft Java process as minecraft.exe
                        }
                    }
                } while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry));
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }
        
        return processes;
    }
    
    /**
     * Check if a javaw.exe process is running Minecraft by examining its command line
     */
    private boolean isMinecraftJavaProcess(int processId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("wmic", "process", "where", 
                "processid=" + processId, "get", "commandline", "/format:list");
            Process process = pb.start();
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("net.minecraft.client.main.Main") || 
                        line.contains("minecraft") || 
                        line.contains("Minecraft")) {
                        return true;
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            logger.debug("Error checking if javaw.exe is Minecraft: {}", e.getMessage());
        }
        return false;
    }
    
    private void terminateProcess(String processName) {
        try {
            if (processName.equalsIgnoreCase("minecraft.exe")) {
                // Terminate both minecraft.exe and any Minecraft Java processes
                terminateMinecraftProcesses();
            } else {
                ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", processName);
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    logger.info("Successfully terminated {}", processName);
                } else {
                    logger.warn("Failed to terminate {} (exit code: {})", processName, exitCode);
                }
            }
        } catch (Exception e) {
            logger.error("Error terminating process {}", processName, e);
        }
    }
    
    /**
     * Terminate all Minecraft-related processes including javaw.exe running Minecraft
     */
    private void terminateMinecraftProcesses() {
        try {
            // First, terminate minecraft.exe processes
            ProcessBuilder pb1 = new ProcessBuilder("taskkill", "/F", "/IM", "minecraft.exe");
            Process process1 = pb1.start();
            process1.waitFor();
            
            // Then, find and terminate Minecraft Java processes
            ProcessBuilder pb2 = new ProcessBuilder("wmic", "process", "where", 
                "name='javaw.exe'", "get", "processid,commandline", "/format:csv");
            Process process2 = pb2.start();
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process2.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if ((line.contains("net.minecraft.client.main.Main") || 
                         line.contains("minecraft") || 
                         line.contains("Minecraft")) && line.contains(",")) {
                        
                        // CSV format: Node,CommandLine,ProcessId
                        // Process ID is the last field after the last comma
                        int lastCommaIndex = line.lastIndexOf(',');
                        if (lastCommaIndex != -1 && lastCommaIndex < line.length() - 1) {
                            try {
                                String processId = line.substring(lastCommaIndex + 1).trim();
                                if (!processId.isEmpty() && processId.matches("\\d+")) {
                                    ProcessBuilder pb3 = new ProcessBuilder("taskkill", "/F", "/PID", processId);
                                    Process process3 = pb3.start();
                                    process3.waitFor();
                                    logger.info("Terminated Minecraft Java process with PID: {}", processId);
                                }
                            } catch (Exception e) {
                                logger.debug("Error parsing process ID from line: {}", line);
                            }
                        }
                    }
                }
            }
            
            process2.waitFor();
            logger.info("Successfully terminated all Minecraft processes");
            
        } catch (Exception e) {
            logger.error("Error terminating Minecraft processes", e);
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
     * Set callback to trigger immediate status updates when process state changes
     */
    public void setStatusUpdateCallback(Runnable callback) {
        this.statusUpdateCallback = callback;
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
    
    /**
     * Check if a specific process is currently running
     */
    public boolean isProcessRunning(String processName) {
        Set<String> runningProcesses = getRunningProcesses();
        return runningProcesses.contains(processName.toLowerCase());
    }
}
