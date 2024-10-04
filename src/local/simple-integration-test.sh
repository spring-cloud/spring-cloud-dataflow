#!/usr/bin/env bash
./mvnw -X clean test-compile failsafe:integration-test -pl spring-cloud-dataflow-server \
  -Pfailsafe -Dgroups=docker-compose -Dtest.docker.compose.pullOnStartup=false  -B --no-transfer-progress
