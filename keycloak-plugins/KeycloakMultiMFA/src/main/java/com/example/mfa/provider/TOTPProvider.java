package com.example.mfa.provider;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import com.example.mfa.config.MFAConfig;

/**
 * Strategy Pattern: Concrete implementation for TOTP MFA
 * Delegates to Keycloak's built-in OTP functionality
 */
public class TOTPProvider extends AbstractMFAProvider {
    
    private static final Logger logger = Logger.getLogger(TOTPProvider.class);
    
    public TOTPProvider(MFAConfig config) {
        super(config);
    }
    
    @Override
    public boolean isConfiguredFor(UserModel user) {
        // Check if user has OTP configured using Keycloak's credential manager
        return user.credentialManager().isConfiguredFor(OTPCredentialModel.TYPE);
    }
    
    @Override
    protected void sendCode(AuthenticationFlowContext context, UserModel user, String code) throws Exception {
        // No need to send a code for TOTP - the user's authenticator app generates it
        logger.info("TOTP validation prepared for user: " + user.getUsername());
    }
    
    @Override
    public boolean verifyCode(AuthenticationFlowContext context, UserModel user, String enteredCode) {
        logger.info("Starting TOTP validation for user: " + user.getUsername());
        
        try {
            // Get OTP credential provider from session
            KeycloakSession session = context.getSession();
            OTPCredentialProvider otpCredProvider = (OTPCredentialProvider) session
                .getProvider(CredentialProvider.class, "keycloak-otp");
            
            // Check if user has OTP configured
            if (!otpCredProvider.isConfiguredFor(context.getRealm(), user)) {
                logger.warn("User does not have TOTP configured: " + user.getUsername());
                return false;
            }
            
            // Get the default credential ID for this user
            String credentialId = otpCredProvider
                .getDefaultCredential(session, context.getRealm(), user)
                .getId();
            
            // Validate the OTP using the credential provider directly
            boolean valid = otpCredProvider.isValid(
                context.getRealm(), 
                user, 
                new UserCredentialModel(credentialId, OTPCredentialModel.TYPE, enteredCode)
            );
            
            logger.info("TOTP validation result for " + user.getUsername() + ": " + valid);
            return valid;
        } catch (Exception e) {
            logger.error("TOTP validation error for " + user.getUsername(), e);
            return false;
        }
    }
    
    @Override
    public boolean configure(AuthenticationFlowContext context, UserModel user, String configValue) {
        // Add the required action to configure TOTP
        logger.info("Adding CONFIGURE_TOTP required action for user: " + user.getUsername());
        user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        
        // We'll return true to indicate we've successfully set up the configuration process
        return true;
    }
    
    /**
     * Check if user should be redirected to TOTP setup
     */
    public boolean shouldSetupTOTP(UserModel user) {
        return !isConfiguredFor(user);
    }
    
    @Override
    public String getType() {
        return "totp";
    }
    
    @Override
    public String getDisplayName() {
        return "Authenticator App";
    }
}