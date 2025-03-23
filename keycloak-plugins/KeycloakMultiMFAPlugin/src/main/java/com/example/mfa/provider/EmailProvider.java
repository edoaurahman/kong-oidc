package com.example.mfa.provider;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;
import com.example.mfa.config.MFAConfig;
import com.example.mfa.service.EmailServiceAdapter;
import com.example.mfa.util.ValidationUtil;

/**
 * Strategy Pattern: Concrete implementation for Email MFA
 */
public class EmailProvider extends AbstractMFAProvider {
    
    private final EmailServiceAdapter emailService;
    
    public EmailProvider(MFAConfig config) {
        super(config);
        this.emailService = EmailServiceAdapter.getInstance(config);
    }
    
    @Override
    public boolean isConfiguredFor(UserModel user) {
        String email = user.getEmail();
        return email != null && !email.isEmpty();
    }
    
    @Override
    protected void sendCode(AuthenticationFlowContext context, UserModel user, String code) throws Exception {
        // We can't use the regular adapter method since we need the Keycloak context
        emailService.sendVerificationCode(context, user, code);
    }
    
    @Override
    public boolean configure(AuthenticationFlowContext context, UserModel user, String email) {
        if (!ValidationUtil.isValidEmail(email)) {
            return false;
        }
        
        user.setEmail(email);
        
        // Check if email verification is required
        boolean requireVerification = Boolean.parseBoolean(
            context.getAuthenticatorConfig().getConfig().getOrDefault("emailVerificationRequired", "true")
        );
        
        // If verification not required, mark as verified
        if (!requireVerification) {
            user.setEmailVerified(true);
        }
        
        return true;
    }
    
    @Override
    public String getType() {
        return "email";
    }
    
    @Override
    public String getDisplayName() {
        return "Email";
    }
}