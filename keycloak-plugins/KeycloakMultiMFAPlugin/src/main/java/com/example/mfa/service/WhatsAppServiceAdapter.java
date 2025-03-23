package com.example.mfa.service;

import com.example.mfa.client.WhatsAppClient;
import com.example.mfa.config.MFAConfig;
import org.keycloak.authentication.AuthenticationFlowContext;

public class WhatsAppServiceAdapter implements ExternalServiceAdapter {
    private final WhatsAppClient client;
    private final MFAConfig config;
    private static WhatsAppServiceAdapter instance;
    public WhatsAppServiceAdapter(MFAConfig config) {
        this.config = config;
        this.client = new WhatsAppClient(config);
    }

    public static synchronized WhatsAppServiceAdapter getInstance(MFAConfig config) {
        if (instance == null) {
            instance = new WhatsAppServiceAdapter(config);
        }
        return instance;
    }

    @Override
    public boolean isConfigured() {
        return config.getWhatsAppXAppKey() != null && config.getWhatsAppXAppToken() != null && config.getWhatsAppEndpoint() != null && config.getWhatsAppMessageTemplate() != null;
    }

    @Override
    public void sendVerificationCode(String recipient, String code) throws Exception {
        // Not implemented
    }

    public void sendVerificationCode(String phoneNumber, String code, AuthenticationFlowContext context) throws Exception {
        String message = subtituteString(config.getWhatsAppMessageTemplate(), "{code}", code);
        message = subtituteString(message, "{username}", getUsername(context));
        client.sendMessage(phoneNumber, message);
    }

    @Override
    public boolean verifyCode(String recipient, String code) {
        return false;
    }

    private String subtituteString(String test, String pattern, String value) {
        return test.replace(pattern, value);
    }

    public String getUsername(AuthenticationFlowContext context) {
        return context.getUser().getUsername();
    }
}
