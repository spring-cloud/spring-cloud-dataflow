#!/bin/bash
if [ "$1" == "" ]; then
    echo "Provide one or more of mariadb, postgres, performance, oauth"
    exit 1
fi
while [ "$1" != "" ]; do
    ./mvnw test -Pfailsafe -Dgroups="$1" -pl spring-cloud-dataflow-server  -B --no-transfer-progress
    shift
done
