#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
./mvnw -o -am -pl :spring-cloud-dataflow-composed-task-runner install -DskipTests -T 0.5C
./mvnw -o -pl :spring-cloud-dataflow-composed-task-runner spring-boot:build-image -DskipTests
