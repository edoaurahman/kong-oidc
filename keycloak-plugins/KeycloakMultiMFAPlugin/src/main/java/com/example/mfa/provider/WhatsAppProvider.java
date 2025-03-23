package com.example.mfa.provider;

import com.example.mfa.config.MFAConfig;
import com.example.mfa.service.WhatsAppServiceAdapter;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;

public class WhatsAppProvider extends AbstractMFAProvider {
    private final WhatsAppServiceAdapter whatsAppService;

    public WhatsAppProvider(MFAConfig config) {
        super(config);
        this.whatsAppService = new WhatsAppServiceAdapter(config);
    }

    @Override
    protected void sendCode(AuthenticationFlowContext context, UserModel user, String code) throws Exception {
        String phoneNumber = user.getFirstAttribute("phoneNumber");
        whatsAppService.sendVerificationCode(phoneNumber, code, context);
    }

    @Override
    public String getType() {
        return "whatsapp";
    }

    @Override
    public String getDisplayName() {
        return "WhatsApp";
    }

    @Override
    public boolean isConfiguredFor(UserModel user) {
        String phoneNumber = user.getFirstAttribute("phoneNumber");
        return phoneNumber != null && !phoneNumber.isEmpty();
    }

    @Override
    public boolean configure(AuthenticationFlowContext context, UserModel user, String configValue) {
        return false;
    }
}
