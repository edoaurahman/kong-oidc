package com.example.mfa.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import com.example.mfa.event.AuthEventManager;
import com.example.mfa.event.LoggingEventListener;
import com.example.mfa.config.MFAConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory untuk membuat instance CustomMFAAuthenticator
 * Mendaftarkan authenticator dengan Keycloak dan menyediakan properti konfigurasi
 */
public class CustomMFAAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    private static final Logger logger = Logger.getLogger(CustomMFAAuthenticatorFactory.class);
    public static final String PROVIDER_ID = "custom-mfa-authenticator";
    private static final CustomMFAAuthenticator SINGLETON = new CustomMFAAuthenticator();

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        // Mendaftarkan event listener logging
        AuthEventManager.getInstance().addEventListener(new LoggingEventListener());

        logger.info("Menginisialisasi properti konfigurasi CustomMFAAuthenticatorFactory");

        // Konfigurasi Twilio - gunakan konstanta string untuk konsistensi nama kunci
        ProviderConfigProperty twilioAccountSid = new ProviderConfigProperty();
        twilioAccountSid.setName("twilioAccountSid");  // Harus cocok dengan kunci di adapter layanan
        twilioAccountSid.setLabel("Twilio Account SID");
        twilioAccountSid.setType(ProviderConfigProperty.STRING_TYPE);
        twilioAccountSid.setHelpText("Twilio Account SID Anda");
        configProperties.add(twilioAccountSid);

        ProviderConfigProperty twilioAuthToken = new ProviderConfigProperty();
        twilioAuthToken.setName("twilioAuthToken");  // Harus cocok dengan kunci di adapter layanan
        twilioAuthToken.setLabel("Twilio Auth Token");
        twilioAuthToken.setType(ProviderConfigProperty.PASSWORD);
        twilioAuthToken.setHelpText("Twilio Auth Token Anda");
        configProperties.add(twilioAuthToken);

        ProviderConfigProperty twilioVerifyServiceSid = new ProviderConfigProperty();
        twilioVerifyServiceSid.setName("twilioVerifyServiceSid");  // Harus cocok dengan kunci di adapter layanan
        twilioVerifyServiceSid.setLabel("Twilio Verify Service SID");
        twilioVerifyServiceSid.setType(ProviderConfigProperty.STRING_TYPE);
        twilioVerifyServiceSid.setHelpText("Twilio Verify Service SID Anda");
        configProperties.add(twilioVerifyServiceSid);

        // Konfigurasi Telegram
        ProviderConfigProperty telegramBotToken = new ProviderConfigProperty();
        telegramBotToken.setName("telegramBotToken");  // Harus cocok dengan kunci di adapter layanan
        telegramBotToken.setLabel("Telegram Bot Token");
        telegramBotToken.setType(ProviderConfigProperty.PASSWORD);
        telegramBotToken.setHelpText("Telegram Bot Token Anda");
        configProperties.add(telegramBotToken);

        // Properti konfigurasi email
        ProviderConfigProperty useKeycloakSmtp = new ProviderConfigProperty();
        useKeycloakSmtp.setName(MFAConfig.USE_KEYCLOAK_SMTP);
        useKeycloakSmtp.setLabel("Gunakan Pengaturan SMTP Keycloak");
        useKeycloakSmtp.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        useKeycloakSmtp.setDefaultValue("true");
        useKeycloakSmtp.setHelpText("Gunakan pengaturan SMTP default Keycloak untuk mengirim email");
        configProperties.add(useKeycloakSmtp);

        ProviderConfigProperty emailVerificationRequired = new ProviderConfigProperty();
        emailVerificationRequired.setName(MFAConfig.EMAIL_VERIFICATION_REQUIRED);
        emailVerificationRequired.setLabel("Wajib Verifikasi Email");
        emailVerificationRequired.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        emailVerificationRequired.setDefaultValue("true");
        emailVerificationRequired.setHelpText("Wajibkan verifikasi email sebelum mengizinkan OTP melalui email");
        configProperties.add(emailVerificationRequired);

        ProviderConfigProperty otpEmailSubject = new ProviderConfigProperty();
        otpEmailSubject.setName(MFAConfig.OTP_EMAIL_SUBJECT);
        otpEmailSubject.setLabel("Subjek Email OTP");
        otpEmailSubject.setType(ProviderConfigProperty.STRING_TYPE);
        otpEmailSubject.setDefaultValue("Kode autentikasi Anda");
        otpEmailSubject.setHelpText("Baris subjek untuk email OTP");
        configProperties.add(otpEmailSubject);

        // Konfigurasi OTP
        ProviderConfigProperty otpExpiration = new ProviderConfigProperty();
        otpExpiration.setName(MFAConfig.OTP_EXPIRATION);
        otpExpiration.setLabel("Waktu Kedaluwarsa OTP");
        otpExpiration.setType(ProviderConfigProperty.STRING_TYPE);
        otpExpiration.setDefaultValue(MFAConfig.DEFAULT_OTP_EXPIRATION); // 5 menit dalam detik
        otpExpiration.setHelpText("Waktu dalam detik sebelum OTP kedaluwarsa");
        configProperties.add(otpExpiration);

        logger.info("Menambahkan " + configProperties.size() + " properti konfigurasi");

        // Konfigurasi WhatsApp
        ProviderConfigProperty whatsAppApiUrl = new ProviderConfigProperty();
        whatsAppApiUrl.setName(MFAConfig.WHATSAPP_API_URL);
        whatsAppApiUrl.setLabel("URL API WhatsApp");
        whatsAppApiUrl.setType(ProviderConfigProperty.STRING_TYPE);
        whatsAppApiUrl.setHelpText("Endpoint API untuk mengirim pesan WhatsApp");
        configProperties.add(whatsAppApiUrl);

        ProviderConfigProperty whatsAppApiKey = new ProviderConfigProperty();
        whatsAppApiKey.setName(MFAConfig.WHATSAPP_API_KEY);
        whatsAppApiKey.setLabel("Kunci API WhatsApp");
        whatsAppApiKey.setType(ProviderConfigProperty.PASSWORD);
        whatsAppApiKey.setHelpText("Nilai header X-APP-KEY untuk autentikasi API WhatsApp");
        configProperties.add(whatsAppApiKey);

        ProviderConfigProperty whatsAppApiToken = new ProviderConfigProperty();
        whatsAppApiToken.setName(MFAConfig.WHATSAPP_API_TOKEN);
        whatsAppApiToken.setLabel("Token API WhatsApp");
        whatsAppApiToken.setType(ProviderConfigProperty.PASSWORD);
        whatsAppApiToken.setHelpText("Nilai header X-APP-TOKEN untuk autentikasi API WhatsApp");
        configProperties.add(whatsAppApiToken);

        ProviderConfigProperty whatsAppMessageTemplate = new ProviderConfigProperty();
        whatsAppMessageTemplate.setName(MFAConfig.WHATSAPP_MESSAGE_TEMPLATE);
        whatsAppMessageTemplate.setLabel("Template Pesan WhatsApp");
        whatsAppMessageTemplate.setType(ProviderConfigProperty.TEXT_TYPE);
        whatsAppMessageTemplate.setHelpText("Format pesan WhatsApp. Gunakan {{code}} untuk kode OTP, {{expiry_time}} untuk waktu kedaluwarsa, dan \\n untuk baris baru");
        whatsAppMessageTemplate.setDefaultValue(
            "Hai,\n" +
            "Kode verifikasi anda adalah {{code}}\n" +
            "Berlaku selama 5 menit sampai dengan pukul {{expiry_time}}\n" +
            "Demi keamanan, mohon untuk tidak membagikan kode ini dengan siapapun.\n" +
            "Terima kasih,\n" +
            "TIM ONEKEY - PETROKIMIA GRESIK"
        );
        configProperties.add(whatsAppMessageTemplate);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Autentikasi MFA Kustom";
    }

    @Override
    public String getReferenceCategory() {
        return "mfa";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // Inisialisasi pengaturan global di sini
        logger.info("Menginisialisasi CustomMFAAuthenticatorFactory");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Lakukan logika pasca-inisialisasi
        logger.info("Pasca-inisialisasi CustomMFAAuthenticatorFactory");
    }

    @Override
    public void close() {
        // Bersihkan sumber daya
        logger.info("Menutup CustomMFAAuthenticatorFactory");
    }

    @Override
    public String getHelpText() {
        return "Menyediakan MFA melalui TOTP, SMS, Telegram, WhatsApp, dan Email";
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }
}