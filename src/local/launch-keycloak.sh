#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

docker run --name keycloak  -p 8080:8080 -p 9000:9000 \
    -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
    -v $SCDIR/data:/opt/keycloak/data/import \
    keycloak/keycloak:25.0 \
    start-dev --import-realm --verbose
