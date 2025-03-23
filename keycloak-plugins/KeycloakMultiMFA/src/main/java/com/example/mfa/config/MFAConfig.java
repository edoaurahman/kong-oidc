package com.example.mfa.config;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatorConfigModel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Kelas konfigurasi untuk MFA dengan properti yang tidak dapat diubah
 */
public class MFAConfig {
    private static final Logger logger = Logger.getLogger(MFAConfig.class);

    // Konfigurasi Email
    public static final String SMTP_HOST = "smtpHost";
    public static final String SMTP_PORT = "smtpPort";
    public static final String SMTP_USERNAME = "smtpUsername";
    public static final String SMTP_PASSWORD = "smtpPassword";
    public static final String SMTP_FROM_EMAIL = "smtpFromEmail";
    public static final String USE_KEYCLOAK_SMTP = "useKeycloakSmtp";
    public static final String EMAIL_VERIFICATION_REQUIRED = "emailVerificationRequired";
    public static final String OTP_EMAIL_SUBJECT = "otpEmailSubject";

    // Konfigurasi WhatsApp
    public static final String WHATSAPP_API_KEY = "whatsAppApiKey";
    public static final String WHATSAPP_API_TOKEN = "whatsAppApiToken";
    public static final String WHATSAPP_API_URL = "whatsAppApiUrl";
    public static final String WHATSAPP_MESSAGE_TEMPLATE = "whatsAppMessageTemplate";

    // Konfigurasi Telegram
    public static final String TELEGRAM_BOT_TOKEN = "telegramBotToken";

    // Konfigurasi Twilio
    public static final String TWILIO_ACCOUNT_SID = "twilioAccountSid";
    public static final String TWILIO_AUTH_TOKEN = "twilioAuthToken";
    public static final String TWILIO_VERIFY_SERVICE_SID = "twilioVerifyServiceSid";

    // Konfigurasi umum
    public static final String OTP_EXPIRATION = "otpExpiration";
    public static final String DEFAULT_OTP_EXPIRATION = "300"; // 5 menit

    private final Map<String, String> config;

    /**
     * Membuat konfigurasi dari model konfigurasi authenticator
     */
    public MFAConfig(AuthenticatorConfigModel configModel) {
        Map<String, String> configMap = new HashMap<>();

        if (configModel != null && configModel.getConfig() != null) {
            logger.info("Model konfigurasi ditemukan dengan " + configModel.getConfig().size() + " properti");
            configMap.putAll(configModel.getConfig());

            // Debug cetak properti WhatsApp utama
            logger.info("WhatsApp API Key dalam konfigurasi: " +
                    (configModel.getConfig().containsKey(WHATSAPP_API_KEY) ? "ada" : "tidak ada"));
            logger.info("WhatsApp API Token dalam konfigurasi: " +
                    (configModel.getConfig().containsKey(WHATSAPP_API_TOKEN) ? "ada" : "tidak ada"));
        } else {
            logger.warn("Model konfigurasi null atau kosong!");
        }

        this.config = Collections.unmodifiableMap(configMap);
    }

    /**
     * Membuat konfigurasi dari peta
     */
    public MFAConfig(Map<String, String> config) {
        Map<String, String> configMap = new HashMap<>();

        if (config != null) {
            configMap.putAll(config);
        }

        this.config = Collections.unmodifiableMap(configMap);
    }

    /**
     * Mendapatkan nilai konfigurasi
     */
    public String getConfig(String key) {
        return config.get(key);
    }

    /**
     * Mendapatkan nilai konfigurasi dengan default
     */
    public String getConfig(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    /**
     * Mendapatkan peta konfigurasi lengkap
     */
    public Map<String, String> getAllConfig() {
        return config;
    }

    // Getter WhatsApp
    public String getWhatsAppApiKey() {
        return getConfig(WHATSAPP_API_KEY);
    }

    public String getWhatsAppApiToken() {
        return getConfig(WHATSAPP_API_TOKEN);
    }

    public String getWhatsAppApiUrl() {
        return getConfig(WHATSAPP_API_URL);
    }

    public String getWhatsAppMessageTemplate() {
        return getConfig(WHATSAPP_MESSAGE_TEMPLATE);
    }

    // Getter Email
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
        return getConfig(OTP_EMAIL_SUBJECT, "Kode autentikasi Anda");
    }

    // Getter Twilio
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

    // Getter Telegram
    public String getTelegramBotToken() {
        return getConfig(TELEGRAM_BOT_TOKEN);
    }

    // Getter umum
    public int getOtpExpiration() {
        String expiration = getConfig(OTP_EXPIRATION);
        return expiration != null ? Integer.parseInt(expiration) : Integer.parseInt(DEFAULT_OTP_EXPIRATION);
    }

    /**
     * Builder untuk membuat konfigurasi kustom untuk pengujian
     */
    public static class Builder {
        private final Map<String, String> config = new HashMap<>();

        public Builder setConfig(String key, String value) {
            config.put(key, value);
            return this;
        }

        // Metode builder WhatsApp
        public Builder setWhatsAppApiKey(String apiKey) {
            return setConfig(WHATSAPP_API_KEY, apiKey);
        }

        public Builder setWhatsAppApiToken(String apiToken) {
            return setConfig(WHATSAPP_API_TOKEN, apiToken);
        }

        public Builder setWhatsAppApiUrl(String apiUrl) {
            return setConfig(WHATSAPP_API_URL, apiUrl);
        }

        public Builder setWhatsAppMessageTemplate(String messageTemplate) {
            return setConfig(WHATSAPP_MESSAGE_TEMPLATE, messageTemplate);
        }

        // Metode builder lainnya...
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

        public MFAConfig build() {
            return new MFAConfig(config);
        }
    }
}