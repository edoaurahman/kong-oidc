.PHONY: build-mfa
build-mfa:
	# Build the Keycloak KeycloakMultiMFAPlugin plugin
	@cd ./keycloak-plugins/KeycloakMultiMFAPlugin && mvn clean package
	# Move the built plugin to the Keycloak distribution
	@mv keycloak-plugins/KeycloakMultiMFAPlugin/target/keycloak-mfa-plugin-*.jar keycloak-providers/
	@echo "KeycloakMultiMFAPlugin plugin built and moved to keycloak-providers/ directory."