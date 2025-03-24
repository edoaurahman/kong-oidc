package com.example.mfa.service;

import com.example.mfa.client.WhatsAppClient;
import com.example.mfa.config.MFAConfig;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
        // Calculate expiry time (OTP_EXPIRATION is in seconds)
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(config.getOtpExpiration());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

        String formattedTime = expiryTime.format(timeFormatter);
        String formattedDate = expiryTime.format(dateFormatter);

        // Get full name
        String fullName = getFullName(context.getUser());

        String message = subtituteString(config.getWhatsAppMessageTemplate(), "{code}", code);
        message = subtituteString(message, "{fullName}", fullName);
        message = subtituteString(message, "{expiryTime}", formattedTime);
        message = subtituteString(message, "{expiryDate}", formattedDate);

        client.sendMessage(phoneNumber, message);
    }

    @Override
    public boolean verifyCode(String recipient, String code) {
        return false;
    }

    private String subtituteString(String test, String pattern, String value) {
        return test.replace(pattern, value);
    }

    private String getFullName(UserModel user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";

        if (firstName.isEmpty() && lastName.isEmpty()) {
            return user.getUsername(); // Fallback to username if no name available
        } else if (firstName.isEmpty()) {
            return lastName;
        } else if (lastName.isEmpty()) {
            return firstName;
        } else {
            return firstName + " " + lastName;
        }
    }
}