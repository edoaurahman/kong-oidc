package com.example.mfa.provider;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;
import com.example.mfa.config.MFAConfig;
import com.example.mfa.service.TwilioServiceAdapter;
import com.example.mfa.util.ValidationUtil;

/**
 * Strategy Pattern: Concrete implementation for SMS MFA
 */
public class SMSProvider extends AbstractMFAProvider {
    
    private final TwilioServiceAdapter twilioService;
    
    public SMSProvider(MFAConfig config) {
        super(config);
        this.twilioService = TwilioServiceAdapter.getInstance(config);
    }
    
    @Override
    public boolean isConfiguredFor(UserModel user) {
        String phoneNumber = user.getFirstAttribute("phoneNumber");
        return phoneNumber != null && !phoneNumber.isEmpty();
    }
    
    @Override
    protected void sendCode(AuthenticationFlowContext context, UserModel user, String code) throws Exception {
        String phoneNumber = user.getFirstAttribute("phoneNumber");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new MFAException("Phone number not configured");
        }
        
        twilioService.sendVerificationCode(phoneNumber, code);
    }
    
    @Override
    public boolean verifyCode(AuthenticationFlowContext context, UserModel user, String code) {
        String phoneNumber = user.getFirstAttribute("phoneNumber");
        return twilioService.verifyCode(phoneNumber, code);
    }
    
    @Override
    public boolean configure(AuthenticationFlowContext context, UserModel user, String phoneNumber) {
        if (!ValidationUtil.isValidPhoneNumber(phoneNumber)) {
            return false;
        }
        
        user.setSingleAttribute("phoneNumber", phoneNumber);
        return true;
    }
    
    @Override
    public String getType() {
        return "sms";
    }
    
    @Override
    public String getDisplayName() {
        return "SMS";
    }
}