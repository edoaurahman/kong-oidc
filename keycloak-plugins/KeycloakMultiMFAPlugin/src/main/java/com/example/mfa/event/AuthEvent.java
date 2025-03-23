package com.example.mfa.event;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;

/**
 * Event class for Observer Pattern
 */
public class AuthEvent {
    
    // Event types
    public enum EventType {
        MFA_SETUP_STARTED,
        MFA_SETUP_COMPLETED,
        MFA_SETUP_FAILED,
        MFA_VERIFICATION_STARTED,
        MFA_VERIFICATION_SUCCEEDED,
        MFA_VERIFICATION_FAILED
    }
    
    private final EventType type;
    private final String mfaMethod;
    private final UserModel user;
    private final AuthenticationFlowContext context;
    private final String details;
    
    private AuthEvent(Builder builder) {
        this.type = builder.type;
        this.mfaMethod = builder.mfaMethod;
        this.user = builder.user;
        this.context = builder.context;
        this.details = builder.details;
    }
    
    public EventType getType() {
        return type;
    }
    
    public String getMfaMethod() {
        return mfaMethod;
    }
    
    public UserModel getUser() {
        return user;
    }
    
    public AuthenticationFlowContext getContext() {
        return context;
    }
    
    public String getDetails() {
        return details;
    }
    
    /**
     * Builder Pattern for creating events
     */
    public static class Builder {
        private EventType type;
        private String mfaMethod;
        private UserModel user;
        private AuthenticationFlowContext context;
        private String details;
        
        public Builder type(EventType type) {
            this.type = type;
            return this;
        }
        
        public Builder mfaMethod(String mfaMethod) {
            this.mfaMethod = mfaMethod;
            return this;
        }
        
        public Builder user(UserModel user) {
            this.user = user;
            return this;
        }
        
        public Builder context(AuthenticationFlowContext context) {
            this.context = context;
            return this;
        }
        
        public Builder details(String details) {
            this.details = details;
            return this;
        }
        
        public AuthEvent build() {
            return new AuthEvent(this);
        }
    }
}