#!/usr/bin/env bash
./mvnw clean install -Pspring
TAG=2.10.0-SNAPSHOT
v=11
pack build \
  --path spring-cloud-dataflow-server/target/spring-cloud-dataflow-server-$TAG.jar \
  --builder gcr.io/paketo-buildpacks/builder:base \
  --env BP_JVM_VERSION=$v springcloud/spring-cloud-dataflow-server:$TAG
pack build \
  --path spring-cloud-dataflow-composed-task-runner/target/spring-cloud-dataflow-composed-task-runner-$TAG.jar \
  --builder gcr.io/paketo-buildpacks/builder:base \
  --env BP_JVM_VERSION=$v springcloud/spring-cloud-dataflow-composed-task-runner:$TAG
pack build \
  --path spring-cloud-dataflow-tasklauncher/spring-cloud-dataflow-tasklauncher-sink-kafka/target/spring-cloud-dataflow-tasklauncher-sink-kafka-$TAG.jar \
  --builder gcr.io/paketo-buildpacks/builder:base \
  --env BP_JVM_VERSION=$v springcloud/spring-cloud-dataflow-tasklauncher-sink-kafka:$TAG
pack build \
  --path spring-cloud-dataflow-tasklauncher/spring-cloud-dataflow-tasklauncher-sink-rabbit/target/spring-cloud-dataflow-tasklauncher-sink-rabbit-$TAG.jar \
  --builder gcr.io/paketo-buildpacks/builder:base \
  --env BP_JVM_VERSION=$v springcloud/spring-cloud-dataflow-tasklauncher-sink-rabbit:$TAG
pack build \
  --path spring-cloud-dataflow-single-step-batch-job/target/spring-cloud-dataflow-single-step-batch-job-$TAG.jar \
  --builder gcr.io/paketo-buildpacks/builder:base \
  --env BP_JVM_VERSION=$v springcloud/spring-cloud-dataflow-single-step-batch-job:$TAG
docker build -t springcloud/spring-cloud-dataflow-prometheus-local:$TAG src/grafana/prometheus/docker/prometheus-local
