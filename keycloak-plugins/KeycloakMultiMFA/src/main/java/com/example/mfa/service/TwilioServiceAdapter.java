package com.example.mfa.service;

import org.jboss.logging.Logger;
import com.example.mfa.config.MFAConfig;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import java.util.Map;

/**
 * Adapter Pattern: Implementation for Twilio service
 * Singleton Pattern: Only one instance per config
 */
public class TwilioServiceAdapter implements ExternalServiceAdapter {
    private static final Logger logger = Logger.getLogger(TwilioServiceAdapter.class);
    
    // Key constants - must match exactly what's in the config
    private static final String KEY_ACCOUNT_SID = "twilioAccountSid";
    private static final String KEY_AUTH_TOKEN = "twilioAuthToken";
    private static final String KEY_SERVICE_SID = "twilioVerifyServiceSid";
    
    private final String verifyServiceSid;
    private final String accountSid;
    private final String authToken;
    private static TwilioServiceAdapter instance;
    
    private TwilioServiceAdapter(MFAConfig config) {
        // Get the raw configuration map for direct access
        Map<String, String> rawConfig = config.getAllConfig();
        
        // Log the available keys for debugging
        logger.info("Available config keys: " + String.join(", ", rawConfig.keySet()));
        
        this.accountSid = rawConfig.get(KEY_ACCOUNT_SID);
        this.authToken = rawConfig.get(KEY_AUTH_TOKEN);
        this.verifyServiceSid = rawConfig.get(KEY_SERVICE_SID);
        
        // Enhanced debugging
        logger.info("Twilio Configuration - AccountSid: " + 
                   (accountSid != null && !accountSid.isEmpty() ? accountSid.substring(0, Math.min(4, accountSid.length())) + "..." : "null") + 
                   ", AuthToken: " + (authToken != null && !authToken.isEmpty() ? "[PRESENT]" : "null") + 
                   ", ServiceSid: " + (verifyServiceSid != null && !verifyServiceSid.isEmpty() ? verifyServiceSid.substring(0, Math.min(4, verifyServiceSid.length())) + "..." : "null"));
        
        if (isConfigured()) {
            logger.info("Twilio is configured, initializing client");
            try {
                Twilio.init(accountSid, authToken);
                logger.info("Twilio client initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Twilio client", e);
            }
        } else {
            logger.info("Twilio isConfigured(): false - running in development mode");
            // Detailed reason for configuration failure
            if (accountSid == null || accountSid.isEmpty()) {
                logger.info("Reason: Account SID is missing");
            }
            if (authToken == null || authToken.isEmpty()) {
                logger.info("Reason: Auth Token is missing");
            }
            if (verifyServiceSid == null || verifyServiceSid.isEmpty()) {
                logger.info("Reason: Verify Service SID is missing");
            }
        }
    }
    
    /**
     * Get singleton instance - reset instance to ensure fresh config
     */
    public static synchronized TwilioServiceAdapter getInstance(MFAConfig config) {
        // Always create a new instance to ensure we have the latest config
        instance = new TwilioServiceAdapter(config);
        return instance;
    }
    
    @Override
    public boolean isConfigured() {
        boolean configured = accountSid != null && !accountSid.isEmpty() 
            && authToken != null && !authToken.isEmpty()
            && verifyServiceSid != null && !verifyServiceSid.isEmpty();
        
        // Log the result and values (carefully with sensitive data)
        logger.debug("isConfigured check: " + configured + 
                    " (AccountSid: " + (accountSid != null ? "present" : "null") + 
                    ", AuthToken: " + (authToken != null ? "present" : "null") + 
                    ", ServiceSid: " + (verifyServiceSid != null ? "present" : "null") + ")");
        
        return configured;
    }
    
    @Override
    public void sendVerificationCode(String phoneNumber, String code) throws Exception {
        if (!isConfigured()) {
            logger.info("Development Mode - Would send verification to: " + phoneNumber + " with code: " + code);
            return;
        }
        
        try {
            Verification verification = Verification.creator(
                verifyServiceSid,
                phoneNumber,
                "sms"
            ).create();
            
            logger.info("Sent verification to " + phoneNumber + ": " + verification.getStatus());
        } catch (Exception e) {
            logger.error("Error sending verification", e);
            throw new RuntimeException("Failed to send verification code", e);
        }
    }
    
    @Override
    public boolean verifyCode(String phoneNumber, String code) {
        if (!isConfigured()) {
            logger.info("Development Mode - Would verify code: " + code + " for " + phoneNumber);
            return true; // Always succeed in development mode
        }
        
        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(
                verifyServiceSid
            ).setTo(phoneNumber)
             .setCode(code)
             .create();
            
            boolean approved = "approved".equals(verificationCheck.getStatus());
            logger.info("Verification result for " + phoneNumber + ": " + verificationCheck.getStatus() + " (approved: " + approved + ")");
            return approved;
        } catch (Exception e) {
            logger.error("Error checking verification", e);
            return false;
        }
    }
}