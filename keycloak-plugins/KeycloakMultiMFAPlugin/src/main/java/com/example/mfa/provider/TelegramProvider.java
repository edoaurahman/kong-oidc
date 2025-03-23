package com.example.mfa.provider;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;
import com.example.mfa.config.MFAConfig;
import com.example.mfa.service.TelegramServiceAdapter;
import com.example.mfa.util.ValidationUtil;

/**
 * Strategy Pattern: Concrete implementation for Telegram MFA
 */
public class TelegramProvider extends AbstractMFAProvider {
    
    private final TelegramServiceAdapter telegramService;
    
    public TelegramProvider(MFAConfig config) {
        super(config);
        this.telegramService = TelegramServiceAdapter.getInstance(config);
    }
    
    @Override
    public boolean isConfiguredFor(UserModel user) {
        String telegramId = user.getFirstAttribute("telegramId");
        return telegramId != null && !telegramId.isEmpty();
    }
    
    @Override
    protected void sendCode(AuthenticationFlowContext context, UserModel user, String code) throws Exception {
        String telegramId = user.getFirstAttribute("telegramId");
        if (telegramId == null || telegramId.isEmpty()) {
            throw new MFAException("Telegram ID not configured");
        }
        
        telegramService.sendVerificationCode(telegramId, code);
    }
    
    @Override
    public boolean configure(AuthenticationFlowContext context, UserModel user, String telegramId) {
        if (!ValidationUtil.isValidTelegramId(telegramId)) {
            return false;
        }
        
        user.setSingleAttribute("telegramId", telegramId);
        return true;
    }
    
    @Override
    public String getType() {
        return "telegram";
    }
    
    @Override
    public String getDisplayName() {
        return "Telegram";
    }
}