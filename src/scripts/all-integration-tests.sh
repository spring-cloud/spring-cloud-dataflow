#!/bin/bash
GROUPS="mariadb postgres oauth performance"
set -e
for GROUP in $GROUPS; do
    ./mvnw test -Pfailsafe -Dgroups=$GROUP -pl spring-cloud-dataflow-server
done
