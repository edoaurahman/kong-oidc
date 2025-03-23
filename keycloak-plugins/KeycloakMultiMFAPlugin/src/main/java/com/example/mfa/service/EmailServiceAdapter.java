package com.example.mfa.service;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.UserModel;
import com.example.mfa.config.MFAConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter Pattern: Implementation for Email service
 * Singleton Pattern: Only one instance per config
 * Bridge Pattern: Bridges between our adapter interface and Keycloak's email provider
 */
public class EmailServiceAdapter implements ExternalServiceAdapter {
    private static final Logger logger = Logger.getLogger(EmailServiceAdapter.class);
    
    private final MFAConfig config;
    private static EmailServiceAdapter instance;
    
    private EmailServiceAdapter(MFAConfig config) {
        this.config = config;
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized EmailServiceAdapter getInstance(MFAConfig config) {
        if (instance == null) {
            instance = new EmailServiceAdapter(config);
        }
        return instance;
    }
    
    @Override
    public boolean isConfigured() {
        // For email, we rely on Keycloak's email configuration
        return true;
    }
    
    @Override
    public void sendVerificationCode(String email, String code) throws Exception {
        // This method is NOT directly usable since we need the Keycloak context
        // It's here to satisfy the interface, but the actual implementation
        // should use the sendVerificationCode method below
        throw new UnsupportedOperationException(
            "This method is not supported. Use sendVerificationCode with AuthenticationFlowContext instead.");
    }
    
    /**
     * Send verification code via email using Keycloak's email provider
     */
    public void sendVerificationCode(AuthenticationFlowContext context, UserModel user, String code) throws EmailException {
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            throw new EmailException("Email not configured");
        }
        
        // Create email content using Builder pattern
        EmailContentBuilder contentBuilder = new EmailContentBuilder()
            .addAttribute("code", code)
            .addAttribute("realmName", context.getRealm().getName())
            .addAttribute("username", user.getUsername());
        
        Map<String, Object> attributes = contentBuilder.build();
        
        // Use Keycloak's email provider
        List<Object> subjectParams = List.of(
            context.getAuthenticatorConfig().getConfig().getOrDefault("otpEmailSubject", "Your authentication code")
        );
        
        EmailTemplateProvider emailProvider = context.getSession().getProvider(EmailTemplateProvider.class);
        if (emailProvider == null) {
            logger.error("Email template provider not found");
            throw new EmailException("Email provider not available");
        }
        
        emailProvider.setRealm(context.getRealm())
                    .setUser(user)
                    .send("Authentication Code", subjectParams, "mfa-otp.ftl", attributes);
                    
        logger.info("Email verification code sent to: " + email);
    }
    
    @Override
    public boolean verifyCode(String email, String code) {
        // Email verification is handled by the AbstractMFAProvider
        return true;
    }
    
    /**
     * Builder Pattern: For creating email content attributes
     */
    public static class EmailContentBuilder {
        private final Map<String, Object> attributes;
        
        public EmailContentBuilder() {
            attributes = new HashMap<>();
        }
        
        public EmailContentBuilder addAttribute(String key, Object value) {
            attributes.put(key, value);
            return this;
        }
        
        public Map<String, Object> build() {
            return attributes;
        }
    }
}