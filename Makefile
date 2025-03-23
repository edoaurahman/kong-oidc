.PHONY: build-mfa
build-mfa:
	# Build the Keycloak KeycloakMultiMFA plugin
	@cd ./keycloak-plugins/KeycloakMultiMFA && mvn clean package
	# Move the built plugin to the Keycloak distribution
	@mv keycloak-plugins/KeycloakMultiMFA/target/keycloak-mfa-plugin-*.jar keycloak-providers/
	@echo "KeycloakMultiMFA plugin built and moved to keycloak-providers/ directory."