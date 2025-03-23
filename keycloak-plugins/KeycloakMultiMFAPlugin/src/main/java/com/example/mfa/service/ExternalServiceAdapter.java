package com.example.mfa.service;

import org.keycloak.authentication.AuthenticationFlowContext;

/**
 * Adapter Pattern: Interface for external service adapters
 */
public interface ExternalServiceAdapter {
    
    /**
     * Check if the service is properly configured
     */
    boolean isConfigured();
    
    /**
     * Send a verification code to the recipient
     */
    void sendVerificationCode(String recipient, String code) throws Exception;

    /**
     * Verify a code for a recipient (if applicable)
     */
    boolean verifyCode(String recipient, String code);
}