#!/bin/bash
GROUP=$1
./mvnw verify -s .settings.xml -Dgroups="$GROUP" -Pfailsafe -pl :spring-cloud-dataflow-server  -B --no-transfer-progress
