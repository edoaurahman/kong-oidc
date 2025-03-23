package com.example.mfa.provider;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;

/**
 * Strategy Pattern: Interface for all MFA providers
 * Each MFA method implements this interface
 */
public interface MFAProvider {
    
    /**
     * Check if this provider is configured for the user
     */
    boolean isConfiguredFor(UserModel user);
    
    /**
     * Send or prepare verification code
     */
    void sendVerificationCode(AuthenticationFlowContext context, UserModel user) throws MFAException;
    
    /**
     * Verify the provided code
     */
    boolean verifyCode(AuthenticationFlowContext context, UserModel user, String code);
    
    /**
     * Configure this provider for the user
     */
    boolean configure(AuthenticationFlowContext context, UserModel user, String configValue);
    
    /**
     * Get the type identifier for this provider
     */
    String getType();
    
    /**
     * Get display name for this provider
     */
    String getDisplayName();
}