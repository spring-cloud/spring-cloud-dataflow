#!/usr/bin/env bash
if [ "$TAG" == "" ]; then
    echo "TAG not found"
    exit 1
fi

function pack_image {
    JAR="$1"
    REPO="$2"
    echo "Packaging $JAR"
    for v in 8 11 17; do

        echo "Creating: $REPO:$TAG-jdk$v"
        pack build --builder gcr.io/paketo-buildpacks/builder:base \
            --path "$JAR-$TAG.jar" \
            --env BP_JVM_VERSION=$v "$REPO:$TAG-jdk$v"
        echo "Created: $REPO:$TAG-jdk$v"

    done
}

pack_image "spring-cloud-dataflow-server/target/spring-cloud-dataflow-server" "springcloud/spring-cloud-dataflow-server"
pack_image "spring-cloud-dataflow-composed-task-runner/target/spring-cloud-dataflow-composed-task-runner" "springcloud/spring-cloud-dataflow-composed-task-runner"
pack_image "spring-cloud-dataflow-tasklauncher/spring-cloud-dataflow-tasklauncher-sink-kafka/target/spring-cloud-dataflow-tasklauncher-sink-kafka" "springcloud/spring-cloud-dataflow-tasklauncher-sink-kafka"
pack_image "spring-cloud-dataflow-tasklauncher/spring-cloud-dataflow-tasklauncher-sink-rabbit/target/spring-cloud-dataflow-tasklauncher-sink-rabbit" "springcloud/spring-cloud-dataflow-tasklauncher-sink-rabbit"
pack_image "spring-cloud-dataflow-single-step-batch-job/target/spring-cloud-dataflow-single-step-batch-job" "springcloud/spring-cloud-dataflow-single-step-batch-job"
