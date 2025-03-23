package com.example.mfa.event;

import org.jboss.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer Pattern: Manager for auth events
 * Singleton Pattern: Single event manager
 */
public class AuthEventManager {
    private static final Logger logger = Logger.getLogger(AuthEventManager.class);
    private static AuthEventManager instance;
    
    private final List<AuthEventListener> listeners = new CopyOnWriteArrayList<>();
    
    private AuthEventManager() {
        // Private constructor for singleton
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized AuthEventManager getInstance() {
        if (instance == null) {
            instance = new AuthEventManager();
        }
        return instance;
    }
    
    /**
     * Add an event listener
     */
    public void addEventListener(AuthEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
            logger.debug("Added auth event listener: " + listener.getClass().getName());
        }
    }
    
    /**
     * Remove an event listener
     */
    public void removeEventListener(AuthEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            logger.debug("Removed auth event listener: " + listener.getClass().getName());
        }
    }
    
    /**
     * Fire an event to all interested listeners
     */
    public void fireEvent(AuthEvent event) {
        if (event == null) {
            return;
        }
        
        logger.debug("Firing auth event: " + event.getType());
        
        for (AuthEventListener listener : listeners) {
            try {
                // Check if this listener is interested in this event type
                if (isListenerInterested(listener, event.getType())) {
                    listener.onEvent(event);
                }
            } catch (Exception e) {
                // Don't let one listener's exception block others
                logger.warn("Exception in auth event listener: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Check if a listener is interested in an event type
     */
    private boolean isListenerInterested(AuthEventListener listener, AuthEvent.EventType eventType) {
        AuthEvent.EventType[] interestedTypes = listener.getInterestedEventTypes();
        return interestedTypes != null && Arrays.asList(interestedTypes).contains(eventType);
    }
}