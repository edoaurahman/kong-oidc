package com.example.mfa.util;

import java.security.SecureRandom;

/**
 * Singleton Pattern: Utility for OTP generation
 */
public class OTPGenerator {
    
    private static OTPGenerator instance;
    private final SecureRandom random;
    
    private OTPGenerator() {
        random = new SecureRandom();
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized OTPGenerator getInstance() {
        if (instance == null) {
            instance = new OTPGenerator();
        }
        return instance;
    }
    
    /**
     * Generate a 6-digit OTP
     */
    public String generateOTP() {
        return String.format("%06d", random.nextInt(1000000));
    }
    
    /**
     * Generate an OTP with specified length
     */
    public String generateOTP(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("OTP length must be positive");
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}