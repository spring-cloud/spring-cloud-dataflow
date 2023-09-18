#!/usr/bin/env bash
if [ "$TAG" == "" ]; then
    echo "TAG not found"
    exit 1
fi
if [ "$DEFAULT_JDK" = "" ]; then
    echo "DEFAULT_JDK not found using 11"
    DEFAULT_JDK=11
else
    echo "DEFAULT_JDK=$DEFAULT_JDK"
fi

function pack_image {
    JAR="$1-$TAG.jar"
    REPO="$2"
    v="$3"
    if [ ! -f "$JAR" ]; then
        echo "File not found $JAR"
        exit 2
    fi
    echo "Creating: $REPO:$TAG-jdk$v"
    pack build --builder gcr.io/paketo-buildpacks/builder:base \
            --path "$JAR" \
            --env BP_JVM_VERSION=$v "$REPO:$TAG-jdk$v"
    RC=$?
    if ((RC!=0)); then
        echo "Error $RC packaging $JAR"
        exit $RC
    fi
    echo "Created: $REPO:$TAG-jdk$v"
}

TARGETS=("spring-cloud-dataflow-server/target/spring-cloud-dataflow-server" \
        "spring-cloud-skipper/spring-cloud-skipper-server/target/spring-cloud-skipper-server" \
        "spring-cloud-dataflow-composed-task-runner/target/spring-cloud-dataflow-composed-task-runner" \
        "spring-cloud-dataflow-single-step-batch-job/target/spring-cloud-dataflow-single-step-batch-job" \
        "spring-cloud-dataflow-tasklauncher/spring-cloud-dataflow-tasklauncher-sink-kafka/target/spring-cloud-dataflow-tasklauncher-sink-kafka" \
        "spring-cloud-dataflow-tasklauncher/spring-cloud-dataflow-tasklauncher-sink-rabbit/target/spring-cloud-dataflow-tasklauncher-sink-rabbit")

IMAGES=("springcloud/spring-cloud-dataflow-server" \
        "springcloud/spring-cloud-skipper-server" \
        "springcloud/spring-cloud-dataflow-composed-task-runner" \
        "springcloud/spring-cloud-dataflow-single-step-batch-job" \
        "springcloud/spring-cloud-dataflow-tasklauncher-sink-kafka" \
        "springcloud/spring-cloud-dataflow-tasklauncher-sink-rabbit")

len=${#TARGETS[@]}
imageLen=${#IMAGES[@]}
if ((len != imageLen)); then
    echo "Expected $len == $imageLen"
    exit 1
fi

for ((i = 0; i < len; i++)); do
    TARGET="${TARGETS[i]}"
    IMAGE="${IMAGES[i]}"
    for v in 8 11 17; do
        pack_image $TARGET $IMAGE $v
        RC=$?
        if [ $RC -ne 0 ]; then
            exit $RC
        fi
        docker push "$IMAGE:$TAG-jdk$v"
        echo "Pushed $IMAGE:$TAG-jdk$v"
        if [ "$DEFAULT_JDK" == "$v" ]; then
            docker tag "$IMAGE:$TAG-jdk$DEFAULT_JDK" "$IMAGE:$TAG"
            docker push "$IMAGE:$TAG"
            echo "Pushed $IMAGE:$TAG"
        fi
    done
    if [ "$IMAGE" == "springcloud/spring-cloud-dataflow-composed-task-runner" ] || [ "$IMAGE" ==  "springcloud/spring-cloud-dataflow-tasklauncher-sink-kafka" ]; then
        echo "Pruning Docker"
        docker system prune -f
        docker system prune --volumes -f
        echo "Sleeping for 5mins"
        sleep 300
    fi
done

docker system prune -f
docker system prune --volumes -f
