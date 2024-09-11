#!/bin/bash
./mvnw install -s .settings.xml -DskipTests -T 1C -am -pl :spring-cloud-dataflow-server,:spring-cloud-skipper-server,:spring-cloud-dataflow-composed-task-runner
./mvnw spring-boot:build-image -s .settings.xml -DskipTests -T 1C -pl :spring-cloud-dataflow-server,:spring-cloud-skipper-server,:spring-cloud-dataflow-composed-task-runner