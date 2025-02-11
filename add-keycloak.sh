!#/bin/bash

docker cp ./keycloak-introspection kong:/usr/local/share/lua/5.1/kong/plugins
docker cp ./kong.conf kong:/etc/kong/kong.conf

docker restart kong

echo "Keycloak plugin added to Kong"