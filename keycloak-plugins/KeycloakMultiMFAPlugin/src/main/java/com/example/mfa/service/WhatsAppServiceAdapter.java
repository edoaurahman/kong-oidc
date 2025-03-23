package com.example.mfa.service;

import com.example.mfa.client.WhatsAppClient;
import com.example.mfa.config.MFAConfig;

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
        return config.getWhatsAppXAppKey() != null && config.getWhatsAppXAppToken() != null && config.getWhatsAppEndpoint() != null;
    }

    @Override
    public void sendVerificationCode(String phoneNumber, String code) throws Exception {
        String message = String.format("Your verification code is: %s", code);
        client.sendMessage(phoneNumber, message);
    }

    @Override
    public boolean verifyCode(String recipient, String code) {
        return false;
    }
}
