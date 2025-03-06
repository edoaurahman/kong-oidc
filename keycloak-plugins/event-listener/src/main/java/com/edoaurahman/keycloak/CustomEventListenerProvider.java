package com.edoaurahman.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

import java.util.StringJoiner;

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

        if (EventType.REGISTER.equals(event.getType())) { // Log Register Event
            log.info("Handling REGISTER event");

            handlingEvent(event, realm);
        } else if (EventType.LOGIN.equals(event.getType())) { // Log Login Event
            log.info("Handling LOGIN event");

            handlingEvent(event, realm);
        }
    }

    private void handlingEvent(Event event, RealmModel realm) {
        event.getDetails().forEach((key, value) -> log.debugf("%s : %s", key, value));

        UserModel user = this.session.users().getUserById(realm, event.getUserId());
        if (user != null) {
            log.infof("User found: %s", user.getUsername());
            sendUserData(user, realm.getName(), event.getClientId(), event.getIpAddress());
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

        if (ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.CREATE.equals(adminEvent.getOperationType())) {
            log.info("Handling USER CREATE admin event");

            UserModel user = this.session.users().getUserById(realm, adminEvent.getResourcePath().substring(6));
            if (user != null) {
                log.infof("User created: %s", user.getUsername());
                sendUserData(user, realm.getName(), adminEvent.getAuthDetails().getClientId(), adminEvent.getAuthDetails().getIpAddress());
            } else {
                log.warnf("User not found for path: %s", adminEvent.getResourcePath());
            }
        } else if (ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.UPDATE.equals(adminEvent.getOperationType())) {
            log.info("Handling USER UPDATE admin event");

            UserModel user = this.session.users().getUserById(realm, adminEvent.getResourcePath().substring(6));
            if (user != null) {
                log.infof("User updated: %s", user.getUsername());
                sendUserData(user, realm.getName(), adminEvent.getAuthDetails().getClientId(), adminEvent.getAuthDetails().getIpAddress());
            } else {
                log.warnf("User not found for path: %s", adminEvent.getResourcePath());
            }
        }
    }

    private void sendUserData(UserModel user, String realmName, String clientId, String ipAddress) {
        String data = """
                {
                    "id": "%s",
                    "email": "%s",
                    "userName": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "realm": "%s",
                    "client": "%s",
                    "ipAddress": "%s",
                }
                """.formatted(user.getId(), user.getEmail(), user.getUsername(), user.getFirstName(), user.getLastName(), realmName, clientId, ipAddress);
        try {
            Client.postService(data);
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
}