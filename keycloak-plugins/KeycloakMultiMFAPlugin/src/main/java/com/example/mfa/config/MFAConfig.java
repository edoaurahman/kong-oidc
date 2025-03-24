package com.example.mfa.config;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatorConfigModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for MFA with immutable properties
 */
public class MFAConfig {
    private static final Logger logger = Logger.getLogger(MFAConfig.class);

    // Email configuration
    public static final String SMTP_HOST = "smtpHost";
    public static final String SMTP_PORT = "smtpPort";
    public static final String SMTP_USERNAME = "smtpUsername";
    public static final String SMTP_PASSWORD = "smtpPassword";
    public static final String SMTP_FROM_EMAIL = "smtpFromEmail";
    public static final String USE_KEYCLOAK_SMTP = "useKeycloakSmtp";
    public static final String EMAIL_VERIFICATION_REQUIRED = "emailVerificationRequired";
    public static final String OTP_EMAIL_SUBJECT = "otpEmailSubject";

    // Telegram configuration
    public static final String TELEGRAM_BOT_TOKEN = "telegramBotToken";

    // Twilio configuration
    public static final String TWILIO_ACCOUNT_SID = "twilioAccountSid";
    public static final String TWILIO_AUTH_TOKEN = "twilioAuthToken";
    public static final String TWILIO_VERIFY_SERVICE_SID = "twilioVerifyServiceSid";

    // General configuration
    public static final String OTP_EXPIRATION = "otpExpiration";
    public static final String DEFAULT_OTP_EXPIRATION = "300"; // 5 minutes

    // Whastapp configuration
    public static final String WHATSAPP_ENDPOINT = "whatsappEndpoint";
    public static final String WHATSAPP_X_APP_KEY = "whatsappXAppKey";
    public static final String WHATSAPP_X_APP_TOKEN = "whatsappXAppToken";
    public static final String WHATSAPP_MESSAGE_TEMPLATE = "whatsappMessageTemplate";
    public static final String DEFAULT_WHATSAPP_MESSAGE_TEMPLATE =
            "Hai {fullName}\n" +
                    "Kode verifikasi anda adalah {code}\n" +
                    "Berlaku selama 5 menit sampai dengan pukul {expirationTime} WIB {expirationDate}\n" +
                    "Demi keamanan, mohon untuk tidak membagikan kode ini dengan siapapun.\n" +
                    "Terima kasih,\n" +
                    "Onekey-Petrokimia Gresik";

    private final Map<String, String> config;

    /**
     * Create a config from an authenticator config model
     */
    public MFAConfig(AuthenticatorConfigModel configModel) {
        Map<String, String> configMap = new HashMap<>();

        if (configModel != null && configModel.getConfig() != null) {
            logger.info("Config model found with " + configModel.getConfig().size() + " properties");
            configMap.putAll(configModel.getConfig());

            // Debug print key Twilio properties
            logger.info("Twilio Account SID in config: " +
                    (configModel.getConfig().containsKey(TWILIO_ACCOUNT_SID) ? "present" : "missing"));
            logger.info("Twilio Auth Token in config: " +
                    (configModel.getConfig().containsKey(TWILIO_AUTH_TOKEN) ? "present" : "missing"));
            logger.info("Twilio Verify Service SID in config: " +
                    (configModel.getConfig().containsKey(TWILIO_VERIFY_SERVICE_SID) ? "present" : "missing"));

            // Debug print key WhatsApp properties
            logger.info("WhatsApp X-App-Key in config: " +
                    (configModel.getConfig().containsKey(WHATSAPP_X_APP_KEY) ? "present" : "missing"));
            logger.info("WhatsApp X-App-Token in config: " +
                    (configModel.getConfig().containsKey(WHATSAPP_X_APP_TOKEN) ? "present" : "missing"));
        } else {
            logger.warn("Config model is null or empty!");
        }

        this.config = Collections.unmodifiableMap(configMap);
    }

    /**
     * Create a config from a map
     */
    public MFAConfig(Map<String, String> config) {
        Map<String, String> configMap = new HashMap<>();

        if (config != null) {
            configMap.putAll(config);
        }

        this.config = Collections.unmodifiableMap(configMap);
    }

    /**
     * Get a configuration value
     */
    public String getConfig(String key) {
        return config.get(key);
    }

    /**
     * Get a configuration value with a default
     */
    public String getConfig(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    /**
     * Get the full configuration map
     */
    public Map<String, String> getAllConfig() {
        return config;
    }

    // Email getters
    public String getSmtpHost() {
        return getConfig(SMTP_HOST);
    }

    public String getSmtpPort() {
        return getConfig(SMTP_PORT);
    }

    public String getSmtpUsername() {
        return getConfig(SMTP_USERNAME);
    }

    public String getSmtpPassword() {
        return getConfig(SMTP_PASSWORD);
    }

    public String getSmtpFromEmail() {
        return getConfig(SMTP_FROM_EMAIL);
    }

    public boolean useKeycloakSmtp() {
        return Boolean.parseBoolean(getConfig(USE_KEYCLOAK_SMTP, "true"));
    }

    public boolean emailVerificationRequired() {
        return Boolean.parseBoolean(getConfig(EMAIL_VERIFICATION_REQUIRED, "true"));
    }

    public String getOtpEmailSubject() {
        return getConfig(OTP_EMAIL_SUBJECT, "Your authentication code");
    }

    // Twilio getters
    public String getTwilioAccountSid() {
        String sid = getConfig(TWILIO_ACCOUNT_SID);
        logger.debug("Retrieved Twilio Account SID: " + (sid != null ? "present" : "null"));
        return sid;
    }

    public String getTwilioAuthToken() {
        return getConfig(TWILIO_AUTH_TOKEN);
    }

    public String getTwilioVerifyServiceSid() {
        return getConfig(TWILIO_VERIFY_SERVICE_SID);
    }

    // Telegram getters
    public String getTelegramBotToken() {
        return getConfig(TELEGRAM_BOT_TOKEN);
    }

    // WhatsApp getters
    public String getWhatsAppXAppKey() {
        return getConfig(WHATSAPP_X_APP_KEY);
    }

    public String getWhatsAppXAppToken() {
        return getConfig(WHATSAPP_X_APP_TOKEN);
    }

    public String getWhatsAppEndpoint() {
        return getConfig(WHATSAPP_ENDPOINT);
    }

    public String getWhatsAppMessageTemplate() {
        return getConfig(WHATSAPP_MESSAGE_TEMPLATE, DEFAULT_WHATSAPP_MESSAGE_TEMPLATE);
    }

    // General getters
    public int getOtpExpiration() {
        String expiration = getConfig(OTP_EXPIRATION);
        return expiration != null ? Integer.parseInt(expiration) : Integer.parseInt(DEFAULT_OTP_EXPIRATION);
    }

    /**
     * Builder for creating custom configs for testing
     */
    public static class Builder {
        private final Map<String, String> config = new HashMap<>();

        public Builder setConfig(String key, String value) {
            config.put(key, value);
            return this;
        }

        public Builder setSmtpHost(String host) {
            return setConfig(SMTP_HOST, host);
        }

        public Builder setSmtpPort(String port) {
            return setConfig(SMTP_PORT, port);
        }

        public Builder setSmtpUsername(String username) {
            return setConfig(SMTP_USERNAME, username);
        }

        public Builder setSmtpPassword(String password) {
            return setConfig(SMTP_PASSWORD, password);
        }

        public Builder setSmtpFromEmail(String email) {
            return setConfig(SMTP_FROM_EMAIL, email);
        }

        public Builder setTwilioAccountSid(String accountSid) {
            return setConfig(TWILIO_ACCOUNT_SID, accountSid);
        }

        public Builder setTwilioAuthToken(String authToken) {
            return setConfig(TWILIO_AUTH_TOKEN, authToken);
        }

        public Builder setTwilioVerifyServiceSid(String serviceSid) {
            return setConfig(TWILIO_VERIFY_SERVICE_SID, serviceSid);
        }

        public Builder setTelegramBotToken(String botToken) {
            return setConfig(TELEGRAM_BOT_TOKEN, botToken);
        }

        public Builder setOtpExpiration(int seconds) {
            return setConfig(OTP_EXPIRATION, String.valueOf(seconds));
        }

        public Builder setWhatsappMessageTemplate(String template) {
            return setConfig(WHATSAPP_MESSAGE_TEMPLATE, template);
        }

        public MFAConfig build() {
            return new MFAConfig(config);
        }
    }
}