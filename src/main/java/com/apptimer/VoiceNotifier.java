package com.apptimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Provides voice notifications using Windows built-in TTS
 * Falls back to system beep if TTS is not available
 */
public class VoiceNotifier {
    private static final Logger logger = LoggerFactory.getLogger(VoiceNotifier.class);
    
    private boolean isTtsAvailable = false;
    
    public VoiceNotifier() {
        checkTtsAvailability();
    }
    
    private void checkTtsAvailability() {
        try {
            // Try to use Windows SAPI for TTS
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", 
                "Add-Type -AssemblyName System.Speech; " +
                "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$speak.Speak('Test')");
            pb.start().waitFor();
            isTtsAvailable = true;
            logger.info("Text-to-Speech is available");
        } catch (Exception e) {
            logger.warn("Text-to-Speech not available, will use system beep", e);
            isTtsAvailable = false;
        }
    }
    
    public void sayWarning(String appName, long remainingTimeSeconds) {
        long remainingMinutes = remainingTimeSeconds / 60;
        String message = String.format("Warning: %s will be closed in %d minutes", 
            appName, Math.max(1, remainingMinutes));
        
        logger.info("Voice warning: {}", message);
        
        CompletableFuture.runAsync(() -> {
            if (isTtsAvailable) {
                speakText(message);
            } else {
                playWarningBeep();
            }
        });
    }
    
    public void sayTimeUp(String appName) {
        String message = String.format("Time limit reached. %s has been closed.", appName);
        
        logger.info("Voice notification: {}", message);
        
        CompletableFuture.runAsync(() -> {
            if (isTtsAvailable) {
                speakText(message);
            } else {
                playTimeUpBeep();
            }
        });
    }
    
    public void announceBlock(String appName, int hours, String reason) {
        String message = String.format("%s has been blocked for %d hours. Reason: %s", 
            appName, hours, reason);
        
        logger.info("Voice notification: {}", message);
        
        CompletableFuture.runAsync(() -> {
            if (isTtsAvailable) {
                speakText(message);
            } else {
                playBlockingBeep();
            }
        });
    }
    
    public void sayBlockedAttempt(String appName, long remainingMinutes) {
        long hours = remainingMinutes / 60;
        long minutes = remainingMinutes % 60;
        
        String message;
        if (hours > 0) {
            message = String.format("%s is blocked. %d hours and %d minutes remaining.", 
                appName, hours, minutes);
        } else {
            message = String.format("%s is blocked. %d minutes remaining.", 
                appName, minutes);
        }
        
        logger.info("Voice notification: {}", message);
        
        CompletableFuture.runAsync(() -> {
            if (isTtsAvailable) {
                speakText(message);
            } else {
                playBlockingBeep();
            }
        });
    }
    
    private void speakText(String text) {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", 
                "Add-Type -AssemblyName System.Speech; " +
                "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$speak.Rate = 0; " +
                "$speak.Volume = 80; " +
                "$speak.Speak('" + text.replace("'", "''") + "')");
            
            Process process = pb.start();
            process.waitFor();
            
        } catch (Exception e) {
            logger.error("Failed to speak text: {}", text, e);
            playWarningBeep(); // Fallback to beep
        }
    }
    
    private void playWarningBeep() {
        try {
            // Play a warning beep pattern
            for (int i = 0; i < 3; i++) {
                playTone(800, 200); // 800Hz for 200ms
                Thread.sleep(100);
            }
        } catch (Exception e) {
            logger.error("Failed to play warning beep", e);
        }
    }
    
    private void playTimeUpBeep() {
        try {
            // Play a more urgent beep pattern
            for (int i = 0; i < 5; i++) {
                playTone(1000, 150); // 1000Hz for 150ms
                Thread.sleep(50);
            }
        } catch (Exception e) {
            logger.error("Failed to play time up beep", e);
        }
    }
    
    private void playBlockingBeep() {
        try {
            // Play a distinctive blocking beep pattern
            for (int i = 0; i < 4; i++) {
                playTone(600, 300); // Lower tone, longer duration
                Thread.sleep(200);
            }
        } catch (Exception e) {
            logger.error("Failed to play blocking beep", e);
        }
    }
    
    private void playTone(int frequency, int duration) throws LineUnavailableException, IOException {
        AudioFormat af = new AudioFormat(8000f, 8, 1, true, false);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
        
        byte[] buffer = new byte[duration * 8];
        for (int i = 0; i < buffer.length; i++) {
            double angle = i / (8000f / frequency) * 2.0 * Math.PI;
            buffer[i] = (byte) (Math.sin(angle) * 127.0);
        }
        
        sdl.write(buffer, 0, buffer.length);
        sdl.drain();
        sdl.close();
    }
    
    public void testVoice() {
        if (isTtsAvailable) {
            speakText("App Time Limiter is working correctly");
        } else {
            logger.info("TTS not available, playing test beep");
            playWarningBeep();
        }
    }
}
