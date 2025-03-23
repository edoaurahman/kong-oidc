package com.example.mfa.provider;

/**
 * Custom exception for MFA operations
 */
public class MFAException extends Exception {
    
    public MFAException(String message) {
        super(message);
    }
    
    public MFAException(String message, Throwable cause) {
        super(message, cause);
    }
}