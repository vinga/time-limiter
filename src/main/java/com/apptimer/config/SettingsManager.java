package com.apptimer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages application settings persistence and loading
 */
public class SettingsManager {
    private static final Logger logger = LoggerFactory.getLogger(SettingsManager.class);
    private static final String SETTINGS_FILE = "app-time-limiter-settings.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Default settings
    public static class AppSettings {
        public int minecraftLimit = 75;      // minutes
        public int chromeLimit = 75;         // minutes  
        public int warningTime = 5;          // minutes
        public int minecraftDelay = 60;      // minutes after time limit
        public int chromeDelay = 60;         // minutes after time limit
        public String passwordHash = "";
        public String lastSaveDate = "";
    }
    
    private AppSettings currentSettings = new AppSettings();
    
    /**
     * Load settings from file
     */
    public AppSettings loadSettings() {
        try {
            Path settingsPath = Paths.get(SETTINGS_FILE);
            if (Files.exists(settingsPath)) {
                String content = Files.readString(settingsPath);
                JsonNode settings = objectMapper.readTree(content);
                
                currentSettings.minecraftLimit = settings.path("minecraftLimit").asInt(75);
                currentSettings.chromeLimit = settings.path("chromeLimit").asInt(75);
                currentSettings.warningTime = settings.path("warningTime").asInt(5);
                currentSettings.minecraftDelay = settings.path("minecraftDelay").asInt(60);
                currentSettings.chromeDelay = settings.path("chromeDelay").asInt(60);
                currentSettings.passwordHash = settings.path("passwordHash").asText("");
                currentSettings.lastSaveDate = settings.path("lastSaveDate").asText("");
                
                logger.info("Enhanced settings loaded: Minecraft={}min ({}min delay), Chrome={}min ({}min delay), Warning={}min", 
                           currentSettings.minecraftLimit, currentSettings.minecraftDelay, 
                           currentSettings.chromeLimit, currentSettings.chromeDelay, currentSettings.warningTime);
            } else {
                logger.info("No settings file found, using enhanced defaults");
            }
        } catch (Exception e) {
            logger.error("Error loading enhanced settings, using defaults", e);
        }
        
        return currentSettings;
    }
    
    /**
     * Save settings to file
     */
    public void saveSettings(AppSettings settings, Map<String, Long> timeUsage) {
        saveSettings(settings, timeUsage, null);
    }
    
    /**
     * Save settings to file with blocked applications state
     */
    public void saveSettings(AppSettings settings, Map<String, Long> timeUsage, Map<String, LocalDateTime> blockedUntil) {
        try {
            ObjectNode settingsNode = objectMapper.createObjectNode();
            settingsNode.put("minecraftLimit", settings.minecraftLimit);
            settingsNode.put("chromeLimit", settings.chromeLimit);
            settingsNode.put("warningTime", settings.warningTime);
            settingsNode.put("minecraftDelay", settings.minecraftDelay);
            settingsNode.put("chromeDelay", settings.chromeDelay);
            settingsNode.put("passwordHash", settings.passwordHash);
            
            // Save time usage data
            if (timeUsage != null) {
                ObjectNode timeUsageNode = objectMapper.createObjectNode();
                for (Map.Entry<String, Long> entry : timeUsage.entrySet()) {
                    timeUsageNode.put(entry.getKey(), entry.getValue());
                }
                settingsNode.set("timeUsage", timeUsageNode);
                
                // Save current date to reset daily
                settingsNode.put("lastSaveDate", java.time.LocalDate.now().toString());
            }
            
            // Save blocked applications state
            if (blockedUntil != null && !blockedUntil.isEmpty()) {
                ObjectNode blockedAppsNode = objectMapper.createObjectNode();
                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                for (Map.Entry<String, LocalDateTime> entry : blockedUntil.entrySet()) {
                    blockedAppsNode.put(entry.getKey(), entry.getValue().format(formatter));
                }
                settingsNode.set("blockedApplications", blockedAppsNode);
                logger.info("Saved blocked applications state: {}", blockedUntil.keySet());
            }
            
            String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(settingsNode);
            Files.writeString(Paths.get(SETTINGS_FILE), content);
            
            logger.info("Enhanced settings and time usage saved to file");
        } catch (Exception e) {
            logger.error("Error saving enhanced settings", e);
        }
    }
    
    /**
     * Load time usage data if from today
     */
    public Map<String, Long> loadTodaysTimeUsage() {
        try {
            Path settingsPath = Paths.get(SETTINGS_FILE);
            if (Files.exists(settingsPath)) {
                String content = Files.readString(settingsPath);
                JsonNode settings = objectMapper.readTree(content);
                
                String lastSaveDate = settings.path("lastSaveDate").asText("");
                String today = java.time.LocalDate.now().toString();
                
                if (today.equals(lastSaveDate) && settings.has("timeUsage")) {
                    JsonNode timeUsage = settings.get("timeUsage");
                    Map<String, Long> usageMap = new java.util.HashMap<>();
                    
                    timeUsage.fieldNames().forEachRemaining(processName -> {
                        long timeSeconds = timeUsage.get(processName).asLong(0);
                        usageMap.put(processName, timeSeconds);
                    });
                    
                    logger.info("Time usage restored from today's session");
                    return usageMap;
                }
            }
        } catch (Exception e) {
            logger.error("Error loading time usage data", e);
        }
        
        logger.info("Starting fresh time tracking (new day or no previous data)");
        return new java.util.HashMap<>();
    }
    
    /**
     * Load blocked applications state
     */
    public Map<String, LocalDateTime> loadBlockedApplications() {
        Map<String, LocalDateTime> blockedUntil = new HashMap<>();
        try {
            Path settingsPath = Paths.get(SETTINGS_FILE);
            if (Files.exists(settingsPath)) {
                String content = Files.readString(settingsPath);
                JsonNode settings = objectMapper.readTree(content);
                
                if (settings.has("blockedApplications")) {
                    JsonNode blockedApps = settings.get("blockedApplications");
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                    LocalDateTime now = LocalDateTime.now();
                    
                    blockedApps.fieldNames().forEachRemaining(appName -> {
                        try {
                            String blockTimeStr = blockedApps.get(appName).asText();
                            LocalDateTime blockUntil = LocalDateTime.parse(blockTimeStr, formatter);
                            
                            // Only restore if block hasn't expired
                            if (now.isBefore(blockUntil)) {
                                blockedUntil.put(appName, blockUntil);
                                logger.info("Restored blocked state for {}: blocked until {}", appName, blockUntil);
                            } else {
                                logger.info("Block for {} has expired, not restoring", appName);
                            }
                        } catch (Exception e) {
                            logger.warn("Error parsing block time for {}: {}", appName, e.getMessage());
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Error loading blocked applications state", e);
        }
        
        return blockedUntil;
    }
    
    public AppSettings getCurrentSettings() {
        return currentSettings;
    }
    
    public void updateSettings(AppSettings newSettings) {
        this.currentSettings = newSettings;
    }
}
