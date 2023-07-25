#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
./mvnw -T 0.5C -o -am -pl :spring-cloud-skipper-server install -DskipTests
./mvnw -o -pl :spring-cloud-skipper-server spring-boot:build-image -DskipTests
