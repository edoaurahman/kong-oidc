package com.example.mfa.service;

import org.jboss.logging.Logger;
import com.example.mfa.config.MFAConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Adapter Pattern: Implementation for Telegram service
 * Singleton Pattern: Only one instance per config
 */
public class TelegramServiceAdapter implements ExternalServiceAdapter {
    private static final Logger logger = Logger.getLogger(TelegramServiceAdapter.class);
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/sendMessage";
    
    // Key constant - must match exactly what's in the config
    private static final String KEY_BOT_TOKEN = "telegramBotToken";
    
    private final String botToken;
    private final HttpClient httpClient;
    private static TelegramServiceAdapter instance;
    
    private TelegramServiceAdapter(MFAConfig config) {
        // Get the raw configuration map for direct access
        Map<String, String> rawConfig = config.getAllConfig();
        
        this.botToken = rawConfig.get(KEY_BOT_TOKEN);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
            
        // Log configuration
        logger.info("Telegram Bot Token: " + (botToken != null && !botToken.isEmpty() ? 
                   botToken.substring(0, Math.min(4, botToken.length())) + "..." : "null"));
        logger.info("Telegram isConfigured(): " + isConfigured());
    }
    
    /**
     * Get singleton instance - reset instance to ensure fresh config
     */
    public static synchronized TelegramServiceAdapter getInstance(MFAConfig config) {
        // Always create a new instance to ensure we have the latest config
        instance = new TelegramServiceAdapter(config);
        return instance;
    }
    
    @Override
    public boolean isConfigured() {
        return botToken != null && !botToken.isEmpty();
    }
    
    @Override
    public void sendVerificationCode(String chatId, String code) throws Exception {
        if (!isConfigured()) {
            logger.info("Development Mode - Telegram OTP for " + chatId + ": " + code);
            return;
        }
        
        try {
            String message = String.format("Your verification code is: %s", code);
            String url = String.format(TELEGRAM_API_URL, botToken);
            
            String jsonBody = String.format(
                "{\"chat_id\": \"%s\", \"text\": \"%s\", \"parse_mode\": \"HTML\"}",
                chatId, message
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.error("Failed to send Telegram message. Status: " + response.statusCode());
                throw new RuntimeException("Failed to send Telegram message");
            }
            
            logger.info("Telegram OTP sent successfully to " + chatId);
        } catch (Exception e) {
            logger.error("Error sending Telegram message", e);
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
    
    @Override
    public boolean verifyCode(String chatId, String code) {
        // Telegram doesn't have a built-in verification API, so we rely on manually comparing codes
        // This is handled by the AbstractMFAProvider's verifyCode method
        return true;
    }
}