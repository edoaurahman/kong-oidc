package com.edoaurahman.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class CustomEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(CustomEventListenerProvider.class);

    private final KeycloakSession session;
    private final RealmProvider model;

    public CustomEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public void onEvent(Event event) {
        log.debugf("New %s Event", event.getType());
        log.debugf("onEvent-> %s", toString(event));

        RealmModel realm = this.model.getRealm(event.getRealmId());
        log.debugf("Realm: %s", realm.getName());
        log.debugf("Client: %s", event.getClientId());

        if (isEventType(event)) {
            handlingEvent(event, realm);
        }
    }

    private void handlingEvent(Event event, RealmModel realm) {
//        event.getDetails().forEach((key, value) -> log.infof("%s : %s", key, value));
        log.infof("Handling event: %s", event.getType().toString());
        UserModel user = this.session.users().getUserById(realm, event.getUserId());
        if (user != null) {
            log.infof("User found: %s", user.getUsername());
            sendUserData(user, realm.getName(), event.getClientId(), event.getIpAddress(), event.getType().toString());
        } else {
            log.warnf("User not found for ID: %s", event.getUserId());
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        log.debug("onEvent(AdminEvent)");
        log.debugf("Resource path: %s", adminEvent.getResourcePath());
        log.debugf("Resource type: %s", adminEvent.getResourceType());
        log.debugf("Operation type: %s", adminEvent.getOperationType());
        log.debugf("AdminEvent.toString(): %s", toString(adminEvent));

        RealmModel realm = this.model.getRealm(adminEvent.getRealmId());
        log.debugf("Realm: %s", realm.getName());
        log.debugf("Client: %s", adminEvent.getAuthDetails().getClientId());

        UserModel user = this.session.users().getUserById(realm, adminEvent.getResourcePath().substring(6));
        if (isAdminEventType(adminEvent)) {
            log.infof("Handling event: %s", user.getUsername());
            sendUserData(user, realm.getName(), adminEvent.getAuthDetails().getClientId(), adminEvent.getAuthDetails().getIpAddress(), adminEvent.getOperationType().toString());
        }
    }

    private void sendUserData(UserModel user, String realmName, String clientId, String ipAddress, String event) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("id", user.getId());
        params.put("email", user.getEmail());
        params.put("userName", user.getUsername());
        params.put("firstName", user.getFirstName());
        params.put("lastName", user.getLastName());
        params.put("realm", realmName);
        params.put("event", event);
        params.put("client", clientId);
        params.put("ipAddress", ipAddress);

        String queryString = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null)  // Filter out null values
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        log.info(queryString);
        try {
            Client.postService(queryString);
            log.debug("A new user has been created and posted to API");
        } catch (Exception e) {
            log.errorf("Failed to call API: %s", e);
        }
    }

    @Override
    public void close() {
        log.debug("Closing CustomEventListenerProvider");
    }

    private String toString(Event event) {
        StringJoiner joiner = new StringJoiner(", ");

        joiner.add("type=" + event.getType())
                .add("realmId=" + event.getRealmId())
                .add("clientId=" + event.getClientId())
                .add("userId=" + event.getUserId())
                .add("ipAddress=" + event.getIpAddress());

        if (event.getError() != null) {
            joiner.add("error=" + event.getError());
        }

        if (event.getDetails() != null) {
            event.getDetails().forEach((key, value) -> {
                if (value == null || !value.contains(" ")) {
                    joiner.add(key + "=" + value);
                } else {
                    joiner.add(key + "='" + value + "'");
                }
            });
        }

        return joiner.toString();
    }

    private String toString(AdminEvent event) {
        RealmModel realm = this.model.getRealm(event.getRealmId());
        UserModel newRegisteredUser = this.session.users().getUserById(realm, event.getAuthDetails().getUserId());

        StringJoiner joiner = new StringJoiner(", ");

        joiner.add("operationType=" + event.getOperationType())
                .add("realmId=" + event.getAuthDetails().getRealmId())
                .add("clientId=" + event.getAuthDetails().getClientId())
                .add("userId=" + event.getAuthDetails().getUserId());

        if (newRegisteredUser != null) {
            joiner.add("email=" + newRegisteredUser.getEmail())
                    .add("username=" + newRegisteredUser.getUsername())
                    .add("firstName=" + newRegisteredUser.getFirstName())
                    .add("lastName=" + newRegisteredUser.getLastName());
        }

        joiner.add("ipAddress=" + event.getAuthDetails().getIpAddress())
                .add("resourcePath=" + event.getResourcePath());

        if (event.getError() != null) {
            joiner.add("error=" + event.getError());
        }

        return joiner.toString();
    }

    private Boolean isEventType(Event event) {
        String eventType = "LOGIN, LOGOUT, RESET_PASSWORD, UPDATE_EMAIL, UPDATE_PROFILE, SEND_RESET_PASSWORD, VERIFY_EMAIL";

        if (eventType.contains(event.getType().toString())) {
            log.infof("Event type: %s", event.getType());
            return true;
        } else {
            log.infof("Event type not found: %s", event.getType());
            return false;
        }
    }

    private Boolean isAdminEventType(AdminEvent event) {
        String eventType = "CLIENT_DELETE, CLIENT_INFO, CLIENT_LOGIN, CLIENT_REGISTER, CLIENT_UPDATE";

        if (eventType.contains(event.getResourceType().toString())) {
            log.infof("Event type: %s", event.getResourceType());
            return true;
        } else {
            log.infof("Event type not found: %s", event.getResourceType());
            return false;
        }
    }
}