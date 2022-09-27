#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
ROOT_DIR=$(realpath $SCDIR/../..)

# set to specific version
if [ "$1" != "" ]; then
    TAG=$1
else
    TAG=2.10.0-SNAPSHOT
fi
if [ "$2" != "" ]; then
    v=$2
else
    v=11
fi

# set with extra option for buildpacks. BP_OPTIONS=

APPS=("spring-cloud-dataflow-server" "spring-cloud-dataflow-composed-task-runner" "spring-cloud-dataflow-single-step-batch-job")
for app in ${APPS[@]}; do
    APP_PATH="$ROOT_DIR/$app/target"
    if [ ! -f "$APP_PATH/$app-$TAG.jar" ]; then
        echo "Cannot find $APP_PATH/$app-$TAG.jar download using download-apps.sh or build using ./mvnw install"
        exit 1
    fi
    pack build \
        --path $APP_PATH/$app-$TAG.jar \
        --builder gcr.io/paketo-buildpacks/builder:base \
        --env BP_JVM_VERSION=$v \
        $BP_OPTIONS springcloud/$app:$TAG
done
TS_APPS=("spring-cloud-dataflow-tasklauncher-sink-kafka" "spring-cloud-dataflow-tasklauncher-sink-rabbit")
for app in ${TS_APPS[@]}; do
    APP_PATH="$ROOT_DIR/spring-cloud-dataflow-tasklauncher/$app/target"
    if [ ! -f "$APP_PATH/$app-$TAG.jar" ]; then
        echo "Cannot find $APP_PATH/$app-$TAG.jar download using download-apps.sh or build using ./mvnw install"
        exit 1
    fi
    pack build \
        --path $APP_PATH/$app-$TAG.jar \
        --builder gcr.io/paketo-buildpacks/builder:base \
        --env BP_JVM_VERSION=$v \
        $BP_OPTIONS springcloud/$app:$TAG
done
pushd $ROOT_DIR > /dev/null
docker build -t springcloud/spring-cloud-dataflow-prometheus-local:$TAG src/grafana/prometheus/docker/prometheus-local
popd > /dev/null
