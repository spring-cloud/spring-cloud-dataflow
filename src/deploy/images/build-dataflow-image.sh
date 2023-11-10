#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
./mvnw -o -am -pl :spring-cloud-dataflow-server install -DskipTests -T 0.5C
./mvnw -o -pl :spring-cloud-dataflow-server spring-boot:build-image -DskipTests
