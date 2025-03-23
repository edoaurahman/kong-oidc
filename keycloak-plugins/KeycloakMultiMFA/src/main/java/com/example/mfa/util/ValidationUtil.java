package com.example.mfa.util;

/**
 * Utility class for validation methods
 */
public class ValidationUtil {
    
    private ValidationUtil() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Validate phone number
     * Requires international format with + prefix
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("\\+[0-9]{10,15}");
    }
    
    /**
     * Validate email address
     * Simple regex validation
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    /**
     * Validate Telegram ID
     * Must be numeric, possibly with a negative sign
     */
    public static boolean isValidTelegramId(String telegramId) {
        return telegramId != null && telegramId.matches("^-?\\d+$");
    }
    
    /**
     * Validate verification code
     * Must be digits only, within specified length
     */
    public static boolean isValidVerificationCode(String code, int expectedLength) {
        return code != null && code.matches("^\\d{" + expectedLength + "}$");
    }
}