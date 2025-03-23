package com.example.mfa.event;

/**
 * Observer Pattern: Interface for event listeners
 */
public interface AuthEventListener {
    
    /**
     * Handle an auth event
     */
    void onEvent(AuthEvent event);
    
    /**
     * Get the types of events this listener is interested in
     */
    AuthEvent.EventType[] getInterestedEventTypes();
}