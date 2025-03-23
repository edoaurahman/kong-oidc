package com.example.mfa.event;

import org.jboss.logging.Logger;

/**
 * Observer Pattern: Example listener that logs all auth events
 */
public class LoggingEventListener implements AuthEventListener {
    private static final Logger logger = Logger.getLogger(LoggingEventListener.class);
    
    @Override
    public void onEvent(AuthEvent event) {
        if (event == null) {
            return;
        }
        
        logger.info(String.format(
            "Auth Event: type=%s, method=%s, user=%s, details=%s",
            event.getType(),
            event.getMfaMethod(),
            event.getUser() != null ? event.getUser().getUsername() : "null",
            event.getDetails() != null ? event.getDetails() : ""
        ));
    }
    
    @Override
    public AuthEvent.EventType[] getInterestedEventTypes() {
        // This listener is interested in all event types
        return AuthEvent.EventType.values();
    }
}