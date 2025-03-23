package com.example.mfa.provider;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;

import com.example.mfa.config.MFAConfig;
import com.example.mfa.service.WhatsAppServiceAdapter;

/**
 * Implementasi MFA Provider untuk WhatsApp
 * Mengikuti Strategy Pattern untuk berbagai metode MFA
 */
public class WhatsAppProvider extends AbstractMFAProvider {
    private static final Logger logger = Logger.getLogger(WhatsAppProvider.class);
    private final WhatsAppServiceAdapter whatsAppService;
    
    // Key for storing the WhatsApp phone number attribute
    private static final String WHATSAPP_PHONE_ATTRIBUTE = "whatsappPhoneNumber";

    public WhatsAppProvider(MFAConfig config) {
        super(config);
        this.whatsAppService = WhatsAppServiceAdapter.getInstance(config);
        logger.info("WhatsAppProvider diinisialisasi, layanan terkonfigurasi: " + whatsAppService.isConfigured());
    }


    @Override
    protected void sendCode(AuthenticationFlowContext context, UserModel user, String code) throws MFAException {
        if (!whatsAppService.isConfigured()) {
            logger.error("WhatsApp service not configured properly");
            throw new MFAException("WhatsApp service not properly configured");
        }
        
        String phoneNumber = user.getFirstAttribute(WHATSAPP_PHONE_ATTRIBUTE);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            logger.error("Phone number not found for user: " + user.getUsername());
            throw new MFAException("Phone number not configured");
        }

        // Format phone number for Indonesian numbers
        phoneNumber = formatIndonesiaPhoneNumber(phoneNumber);
        
        try {
            whatsAppService.sendVerificationCode(phoneNumber, code);
            logger.info("WhatsApp verification code sent to: " + phoneNumber);
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp verification code", e);
            throw new MFAException("Failed to send WhatsApp verification code: " + e.getMessage());
        }
    }


    /**
     * Format nomor telepon Indonesia dengan benar
     * - Jika dimulai dengan "0", ganti dengan "+62"
     * - Jika dimulai dengan "62", tambahkan "+"
     * - Jika belum memiliki "+", tambahkan
     */
    private String formatIndonesiaPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }

        phoneNumber = phoneNumber.trim();
        
        // Jika dimulai dengan 0, ganti dengan +62
        if (phoneNumber.startsWith("0")) {
            phoneNumber = "+62" + phoneNumber.substring(1);
        } 
        // Jika dimulai dengan 62 (tanpa +), tambahkan +
        else if (phoneNumber.startsWith("62") && !phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }
        // Jika tidak dimulai dengan +, tambahkan +
        else if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }
        
        logger.debug("Nomor telepon diformat dari asli menjadi: " + phoneNumber);
        return phoneNumber;
    }

    @Override
    public boolean isConfiguredFor(UserModel user) {
        // Periksa apakah pengguna memiliki atribut nomor telepon
        String phoneNumber = user.getFirstAttribute("PhoneNumber");
        boolean isConfigured = phoneNumber != null && !phoneNumber.trim().isEmpty();

        // Log hasil
        logger.debug("WhatsApp MFA dikonfigurasi untuk pengguna " + user.getUsername() + ": " + isConfigured);

        return isConfigured;
    }

    @Override
    public boolean configure(AuthenticationFlowContext context, UserModel user, String phoneNumber) {
        try {
            // Validasi format nomor telepon
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                logger.error("Nomor telepon tidak valid disediakan untuk konfigurasi");
                return false;
            }

            // Format nomor telepon Indonesia dengan benar
            phoneNumber = formatIndonesiaPhoneNumber(phoneNumber);

            // Set atribut nomor telepon untuk pengguna
            user.setSingleAttribute("PhoneNumber", phoneNumber); // Ganti menjadi 'P' kapital
            
            logger.info("WhatsApp MFA dikonfigurasi untuk pengguna: " + user.getUsername());
            return true;
        } catch (Exception e) {
            logger.error("Gagal mengonfigurasi WhatsApp MFA", e);
            return false;
        }
    }
    
    @Override
    public String getType() {
        return "whatsapp";
    }

    @Override
    public String getDisplayName() {
        return "WhatsApp";
    }
}