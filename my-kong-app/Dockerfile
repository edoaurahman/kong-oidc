FROM kong:3.9.0

USER kong
WORKDIR /usr/local/share/lua/5.1/kong/plugins
COPY ./keycloak-introspection ./keycloak-introspection
COPY ./kong.conf /etc/kong/kong.conf
