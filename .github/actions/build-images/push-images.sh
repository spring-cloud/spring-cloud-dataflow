#!/usr/bin/env bash
if [ "$TAG" == "" ]; then
    echo "TAG not found"
    exit 1
fi

function push_image {
    REPO="$1"
    docker tag "$REPO:$TAG-jdk$DEFAULT_JDK" "$REPO:$TAG"
    for v in 8 11 17; do
        docker push "$REPO:$TAG-jdk$v"
    done
    docker push "$REPO:$TAG"
}

push_image "springcloud/spring-cloud-dataflow-server"
push_image "springcloud/spring-cloud-dataflow-composed-task-runner"
push_image "springcloud/spring-cloud-dataflow-tasklauncher-sink-kafka"
push_image "springcloud/spring-cloud-dataflow-tasklauncher-sink-rabbit"
push_image "springcloud/spring-cloud-dataflow-single-step-batch-job"
push_image "springcloud/spring-cloud-skipper-server"
