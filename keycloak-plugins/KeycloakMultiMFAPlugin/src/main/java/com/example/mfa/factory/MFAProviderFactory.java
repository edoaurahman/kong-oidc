package com.example.mfa.factory;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatorConfigModel;

import com.example.mfa.config.MFAConfig;
import com.example.mfa.provider.*;

/**
 * Factory Pattern: Creates MFA provider instances based on type
 * Singleton Pattern: Single factory instance
 */
public class MFAProviderFactory {
    private static final Logger logger = Logger.getLogger(MFAProviderFactory.class);
    private static MFAProviderFactory instance;

    private MFAProviderFactory() {
        // Private constructor for singleton
    }

    /**
     * Get singleton instance
     */
    public static synchronized MFAProviderFactory getInstance() {
        if (instance == null) {
            instance = new MFAProviderFactory();
        }
        return instance;
    }

    /**
     * Create an MFA provider based on the type and configuration
     */
    public MFAProvider createProvider(String type, AuthenticatorConfigModel configModel) {
        if (type == null) {
            throw new IllegalArgumentException("MFA provider type cannot be null");
        }

        MFAConfig config = new MFAConfig(configModel);

        switch (type.toLowerCase()) {
            case "sms":
                return new SMSProvider(config);

            case "email":
                return new EmailProvider(config);

            case "telegram":
                return new TelegramProvider(config);

            case "totp":
                return new TOTPProvider(config);

            case "whatsapp":
                return new WhatsAppProvider(config);

            default:
                logger.warn("Unknown MFA provider type: " + type);
                throw new IllegalArgumentException("Unknown MFA provider type: " + type);
        }
    }
}