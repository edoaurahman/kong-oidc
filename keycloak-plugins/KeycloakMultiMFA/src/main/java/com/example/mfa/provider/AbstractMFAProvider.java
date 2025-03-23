package com.example.mfa.provider;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;
import com.example.mfa.config.MFAConfig;
import com.example.mfa.util.OTPGenerator;

/**
 * Template Method Pattern: Abstract base class for MFA providers
 * Implements common functionality and defines template methods for specialization
 */
public abstract class AbstractMFAProvider implements MFAProvider {
    
    protected static final Logger logger = Logger.getLogger(AbstractMFAProvider.class);
    protected final MFAConfig config;
    protected final OTPGenerator otpGenerator;
    
    public AbstractMFAProvider(MFAConfig config) {
        this.config = config;
        this.otpGenerator = OTPGenerator.getInstance();
    }
    
    /**
     * Template method for sending verification code
     */
    @Override
    public void sendVerificationCode(AuthenticationFlowContext context, UserModel user) throws MFAException {
        try {
            // Generate OTP code
            String otp = generateCode();
            
            // Store OTP in session for verification
            storeCodeInSession(context, otp);
            
            // Perform provider-specific sending
            sendCode(context, user, otp);
            
            logger.info("Verification code sent via " + getType() + " to user: " + user.getUsername());
        } catch (Exception e) {
            logger.error("Failed to send verification code via " + getType(), e);
            throw new MFAException("Failed to send verification code: " + e.getMessage(), e);
        }
    }
    
    /**
     * Default implementation of code verification
     */
    @Override
    public boolean verifyCode(AuthenticationFlowContext context, UserModel user, String code) {
        String storedCode = getCodeFromSession(context);
        return storedCode != null && storedCode.equals(code);
    }
    
    /**
     * Provider-specific implementation for sending code
     */
    protected abstract void sendCode(AuthenticationFlowContext context, UserModel user, String code) throws Exception;
    
    /**
     * Generate verification code
     */
    protected String generateCode() {
        return otpGenerator.generateOTP();
    }
    
    /**
     * Store code in session
     */
    protected void storeCodeInSession(AuthenticationFlowContext context, String code) {
        context.getAuthenticationSession().setAuthNote("otp_code", code);
    }
    
    /**
     * Get code from session
     */
    protected String getCodeFromSession(AuthenticationFlowContext context) {
        return context.getAuthenticationSession().getAuthNote("otp_code");
    }
}