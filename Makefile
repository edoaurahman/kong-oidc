.PHONY: build-mfa
build-mfa:
	# Build the Keycloak KeycloakMultiMFAPlugin plugin
	@cd ./keycloak-plugins/KeycloakMultiMFAPlugin && mvn clean package
	# Move the built plugin to the Keycloak distribution
	@mv keycloak-plugins/KeycloakMultiMFAPlugin/target/keycloak-mfa-plugin-*.jar keycloak-providers/
	@echo "KeycloakMultiMFAPlugin plugin built and moved to keycloak-providers/ directory."

build-event-listener:
	# Build the Keycloak EventListener plugin
	@cd ./keycloak-plugins/event-listener && mvn clean package
	# Move the built plugin to the Keycloak distribution
	@mv ./keycloak-plugins/event-listener/target/custom-event-listener.jar keycloak-providers/
	@echo "event-listener plugin built and moved to keycloak-providers/ directory."