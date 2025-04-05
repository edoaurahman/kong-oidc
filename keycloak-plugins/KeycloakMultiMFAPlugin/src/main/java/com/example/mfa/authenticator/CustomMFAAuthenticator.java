package com.example.mfa.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import com.example.mfa.config.MFAConfig;
import com.example.mfa.event.AuthEvent;
import com.example.mfa.event.AuthEventManager;
import com.example.mfa.factory.MFAProviderFactory;
import com.example.mfa.provider.MFAException;
import com.example.mfa.provider.MFAProvider;
import com.example.mfa.provider.TOTPProvider;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Main authenticator class refactored to use multiple design patterns:
 * - Strategy Pattern: Delegates to different MFA providers
 * - Factory Pattern: Uses factory to create providers
 * - Observer Pattern: Fires events during authentication
 * - Template Method Pattern: Defines authentication flow
 */
public class CustomMFAAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(CustomMFAAuthenticator.class);

    // Templates
    private static final String TEMPLATE_SELECT = "mfa-select.ftl";
    private static final String TEMPLATE_CONFIG = "mfa-config.ftl";
    private static final String TEMPLATE_CODE = "mfa-code.ftl";

    // Auth states
    private static final String AUTH_STATE = "auth_state";
    private static final String STATE_METHOD_SELECT = "METHOD_SELECT";
    private static final String STATE_METHOD_CONFIG = "METHOD_CONFIG";
    private static final String STATE_CODE_VALIDATION = "CODE_VALIDATION";

    // Session notes
    private static final String NOTE_CHOSEN_METHOD = "chosen_method";

    private final MFAProviderFactory providerFactory;
    private final AuthEventManager eventManager;

    public CustomMFAAuthenticator() {
        this.providerFactory = MFAProviderFactory.getInstance();
        this.eventManager = AuthEventManager.getInstance();
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            context.failure(AuthenticationFlowError.UNKNOWN_USER);
            return;
        }

        String state = context.getAuthenticationSession().getAuthNote(AUTH_STATE);
        if (state == null) {
            showMethodSelection(context, user);
            return;
        }

        switch (state) {
            case STATE_METHOD_CONFIG:
                showMethodConfiguration(context, user);
                break;
            case STATE_CODE_VALIDATION:
                String method = context.getAuthenticationSession().getAuthNote(NOTE_CHOSEN_METHOD);
                try {
                    MFAProvider provider = providerFactory.createProvider(method, context.getAuthenticatorConfig());

                    if (!provider.isConfiguredFor(user)) {
                        logger.warn("User attempted to use MFA method that's not configured: " + user.getUsername());
                        context.form().setError("configError", "MFA method not properly configured");
                        showMethodSelection(context, user);
                        return;
                    }

                    // Fire event
                    fireVerificationStartedEvent(context, user, method);

                    context.form().setAttribute("method", method);

                    // Send verification code
                    provider.sendVerificationCode(context, user);
                    context.challenge(context.form().createForm(TEMPLATE_CODE));
                } catch (MFAException e) {
                    logger.error("Error sending verification code", e);
                    context.form().setError("sendError", "Failed to send verification code");
                    showMethodSelection(context, user);
                }
                break;
            default:
                showMethodSelection(context, user);
                break;
        }
    }

    private void showMethodSelection(AuthenticationFlowContext context, UserModel user) {
        context.getAuthenticationSession().setAuthNote(AUTH_STATE, STATE_METHOD_SELECT);

        // Check which methods are configured
        boolean smsConfigured = isMethodConfigured(user, "sms");
        boolean telegramConfigured = isMethodConfigured(user, "telegram");
        boolean emailConfigured = isMethodConfigured(user, "email");
        boolean totpConfigured = isMethodConfigured(user, "totp");
        boolean whatsappConfigured = isMethodConfigured(user, "whatsapp");

        context.form()
                .setAttribute("sms_configured", smsConfigured)
                .setAttribute("telegram_configured", telegramConfigured)
                .setAttribute("email_configured", emailConfigured)
                .setAttribute("totp_configured", totpConfigured)
                .setAttribute("whatsapp_configured", whatsappConfigured);

        context.challenge(context.form().createForm(TEMPLATE_SELECT));
    }

    private boolean isMethodConfigured(UserModel user, String method) {
        try {
            MFAProvider provider = providerFactory.createProvider(method, null);
            return provider.isConfiguredFor(user);
        } catch (Exception e) {
            logger.warn("Error checking if " + method + " is configured", e);
            return false;
        }
    }

    private void showMethodConfiguration(AuthenticationFlowContext context, UserModel user) {
        String method = context.getAuthenticationSession().getAuthNote(NOTE_CHOSEN_METHOD);
        if (method == null) {
            showMethodSelection(context, user);
            return;
        }

        context.form().setAttribute("method", method);
        context.challenge(context.form().createForm(TEMPLATE_CONFIG));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        String state = context.getAuthenticationSession().getAuthNote(AUTH_STATE);

        if (state == null || user == null) {
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        switch (state) {
            case STATE_METHOD_SELECT:
                handleMethodSelection(context, user, formData);
                break;
            case STATE_METHOD_CONFIG:
                handleMethodConfiguration(context, user, formData);
                break;
            case STATE_CODE_VALIDATION:
                handleCodeValidation(context, user, formData);
                break;
            default:
                context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    private void handleMethodSelection(AuthenticationFlowContext context, UserModel user,
                                       MultivaluedMap<String, String> formData) {
        String method = formData.getFirst("mfa-method");
        if (method == null) {
            showMethodSelection(context, user);
            return;
        }

        try {
            MFAProvider provider = providerFactory.createProvider(method, context.getAuthenticatorConfig());
            context.getAuthenticationSession().setAuthNote(NOTE_CHOSEN_METHOD, method);

            if ("totp".equals(method)) {
                TOTPProvider totpProvider = (TOTPProvider) provider;
                if (totpProvider.shouldSetupTOTP(user)) {
                    logger.info("TOTP not configured for user " + user.getUsername() + ", adding required action");

                    // Add required action and redirect to TOTP setup
                    user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);

                    // Fire setup event
                    fireSetupStartedEvent(context, user, method);

                    // Complete this authenticator and forward to required actions
                    context.success();
                    return;
                }
            }

            if (provider.isConfiguredFor(user)) {
                context.getAuthenticationSession().setAuthNote(AUTH_STATE, STATE_CODE_VALIDATION);

                // Fire event
                fireVerificationStartedEvent(context, user, method);

                provider.sendVerificationCode(context, user);
                context.form().setAttribute("method", method);
                context.challenge(context.form().createForm(TEMPLATE_CODE));
            } else {
                context.getAuthenticationSession().setAuthNote(AUTH_STATE, STATE_METHOD_CONFIG);
                showMethodConfiguration(context, user);
            }
        } catch (Exception e) {
            logger.error("Error during method selection", e);
            context.form().setError("configError", "Error configuring MFA method");
            showMethodSelection(context, user);
        }
    }

    private void handleMethodConfiguration(AuthenticationFlowContext context, UserModel user,
                                           MultivaluedMap<String, String> formData) {
        String method = context.getAuthenticationSession().getAuthNote(NOTE_CHOSEN_METHOD);
        if (method == null) {
            showMethodSelection(context, user);
            return;
        }

        try {
            MFAProvider provider = providerFactory.createProvider(method, context.getAuthenticatorConfig());

            // Get the appropriate config value from form data based on method
            String configValue = null;
            switch (method) {
                case "sms":
                case "whatsapp":
                    configValue = formData.getFirst("PhoneNumber");
                    break;
                case "telegram":
                    configValue = formData.getFirst("telegramId");
                    break;
                case "email":
                    configValue = formData.getFirst("email");
                    break;
                case "totp":
                    // TOTP config is handled by Keycloak's built-in flow
                    configValue = "";
                    break;
            }

            if (configValue == null) {
                context.form().setError("configError", "Missing configuration value");
                showMethodConfiguration(context, user);
                return;
            }

            boolean configured = provider.configure(context, user, configValue);

            if (configured) {
                // Fire event
                fireSetupCompletedEvent(context, user, method);

                context.getAuthenticationSession().setAuthNote(AUTH_STATE, STATE_CODE_VALIDATION);
                context.form().setAttribute("method", method);
                provider.sendVerificationCode(context, user);
                context.challenge(context.form().createForm(TEMPLATE_CODE));
            } else {
                context.form().setError("configError", "Invalid configuration value");
                showMethodConfiguration(context, user);
            }
        } catch (Exception e) {
            logger.error("Error during method configuration", e);
            context.form().setError("configError", "Error configuring MFA method");
            showMethodConfiguration(context, user);
        }
    }

    private void handleCodeValidation(AuthenticationFlowContext context, UserModel user,
                                      MultivaluedMap<String, String> formData) {
        String enteredCode = formData.getFirst("code");
        if (enteredCode == null || enteredCode.trim().isEmpty()) {
            context.form().setError("invalidCode", "Invalid verification code");
            context.challenge(context.form().createForm(TEMPLATE_CODE));
            return;
        }

        String method = context.getAuthenticationSession().getAuthNote(NOTE_CHOSEN_METHOD);

        try {
            MFAProvider provider = providerFactory.createProvider(method, context.getAuthenticatorConfig());
            boolean isValid = provider.verifyCode(context, user, enteredCode);

            if (isValid) {
                // Fire event
                fireVerificationSucceededEvent(context, user, method);

                context.success();
            } else {
                // Fire event
                fireVerificationFailedEvent(context, user, method, "Invalid code");

                context.form().setError("invalidCode", "Invalid verification code");
                context.form().setAttribute("method", method);
                context.challenge(context.form().createForm(TEMPLATE_CODE));
            }
        } catch (Exception e) {
            logger.error("Error during code validation", e);
            context.form().setError("validationError", "Error validating code");
            context.form().setAttribute("method", method);
            context.challenge(context.form().createForm(TEMPLATE_CODE));
        }
    }

    // Event firing methods
    private void fireSetupStartedEvent(AuthenticationFlowContext context, UserModel user, String method) {
        AuthEvent event = new AuthEvent.Builder()
                .type(AuthEvent.EventType.MFA_SETUP_STARTED)
                .mfaMethod(method)
                .user(user)
                .context(context)
                .build();

        eventManager.fireEvent(event);
    }

    private void fireSetupCompletedEvent(AuthenticationFlowContext context, UserModel user, String method) {
        AuthEvent event = new AuthEvent.Builder()
                .type(AuthEvent.EventType.MFA_SETUP_COMPLETED)
                .mfaMethod(method)
                .user(user)
                .context(context)
                .build();

        eventManager.fireEvent(event);
    }

    private void fireSetupFailedEvent(AuthenticationFlowContext context, UserModel user, String method, String reason) {
        AuthEvent event = new AuthEvent.Builder()
                .type(AuthEvent.EventType.MFA_SETUP_FAILED)
                .mfaMethod(method)
                .user(user)
                .context(context)
                .details(reason)
                .build();

        eventManager.fireEvent(event);
    }

    private void fireVerificationStartedEvent(AuthenticationFlowContext context, UserModel user, String method) {
        AuthEvent event = new AuthEvent.Builder()
                .type(AuthEvent.EventType.MFA_VERIFICATION_STARTED)
                .mfaMethod(method)
                .user(user)
                .context(context)
                .build();

        eventManager.fireEvent(event);
    }

    private void fireVerificationSucceededEvent(AuthenticationFlowContext context, UserModel user, String method) {
        AuthEvent event = new AuthEvent.Builder()
                .type(AuthEvent.EventType.MFA_VERIFICATION_SUCCEEDED)
                .mfaMethod(method)
                .user(user)
                .context(context)
                .build();

        eventManager.fireEvent(event);
    }

    private void fireVerificationFailedEvent(AuthenticationFlowContext context, UserModel user, String method, String reason) {
        AuthEvent event = new AuthEvent.Builder()
                .type(AuthEvent.EventType.MFA_VERIFICATION_FAILED)
                .mfaMethod(method)
                .user(user)
                .context(context)
                .details(reason)
                .build();

        eventManager.fireEvent(event);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // Always return true as we handle unconfigured cases in the flow
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions to set here
    }

    @Override
    public void close() {
        // No resources to close
    }
}