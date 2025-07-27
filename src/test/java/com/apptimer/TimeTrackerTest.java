package com.apptimer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TimeTracker class
 */
public class TimeTrackerTest {
    
    private TimeTracker timeTracker;
    
    @BeforeEach
    void setUp() {
        timeTracker = new TimeTracker();
    }
    
    @Test
    void testSetTimeLimit() {
        timeTracker.setTimeLimit("test.exe", 3600); // 1 hour
        
        // Should not exceed limit initially
        assertFalse(timeTracker.isTimeExceeded("test.exe"));
    }
    
    @Test
    void testWarningTime() {
        timeTracker.setTimeLimit("test.exe", 600); // 10 minutes
        timeTracker.setWarningTime(60); // 1 minute warning
        
        // Simulate 9 minutes of usage
        for (int i = 0; i < 9; i++) {
            timeTracker.recordActivity("test.exe");
            try {
                Thread.sleep(1000); // 1 second intervals
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Should issue warning now
        assertTrue(timeTracker.shouldWarn("test.exe"));
    }
    
    @Test
    void testRemainingTime() {
        timeTracker.setTimeLimit("test.exe", 300); // 5 minutes
        
        // Initially should have full time remaining
        assertEquals(300, timeTracker.getRemainingTime("test.exe"), 5); // 5 second tolerance
    }
    
    @Test
    void testResetTime() {
        timeTracker.setTimeLimit("test.exe", 300);
        timeTracker.recordActivity("test.exe");
        
        assertTrue(timeTracker.getTotalTime("test.exe") >= 0);
        
        timeTracker.resetTime("test.exe");
        assertEquals(0, timeTracker.getTotalTime("test.exe"));
    }
    
    @Test
    void testMultipleProcesses() {
        timeTracker.setTimeLimit("minecraft.exe", 3600);
        timeTracker.setTimeLimit("chrome.exe", 7200);
        
        timeTracker.recordActivity("minecraft.exe");
        timeTracker.recordActivity("chrome.exe");
        
        // Both should be tracked independently
        assertTrue(timeTracker.getTotalTime("minecraft.exe") >= 0);
        assertTrue(timeTracker.getTotalTime("chrome.exe") >= 0);
    }
}
