#!/usr/bin/env bash
if [ -n "$BASH_SOURCE" ]; then
  SCDIR="$(readlink -f "${BASH_SOURCE[0]}")"
elif [ -n "$ZSH_VERSION" ]; then
  setopt function_argzero
  SCDIR="${(%):-%N}"
elif eval '[[ -n ${.sh.file} ]]' 2>/dev/null; then
  eval 'SCDIR=${.sh.file}'
else
  echo 1>&2 "Unsupported shell. Please use bash, ksh93 or zsh."
    exit 2
fi
SCDIR="$(dirname "$SCDIR")"

ROOT_DIR=$(realpath "$SCDIR/../..")

# set to specific version
if [ "$1" != "" ]; then
    TAG=$1
else
    TAG=2.11.3-SNAPSHOT
fi
if [ "$2" != "" ]; then
    v=$2
else
    v=11
fi
PROCESSOR=$(uname -p)
# export ARCH=arm64v8 for ARM64 image
if [ "$ARCH" == "" ]; then
    case $PROCESSOR in
    "x86_64")
        ARCH=amd64
        ;;
    *)
        if [[ "$PROCESSOR" == *"arm"* ]]; then
            ARCH=arm64v8
        fi
        ;;
    esac
fi
CRED=
if [ "$DOCKER_USERNAME" != "" ]; then
    echo "Using $DOCKER_USERNAME for docker.io"
    CRED="--from-username=$DOCKER_USERNAME --from-password=$DOCKER_PASSWORD"
fi
# set with extra option for buildpacks. BP_OPTIONS=
IMAGE="$ARCH/eclipse-temurin:$v-jdk-jammy"
APPS=("spring-cloud-dataflow-server" "spring-cloud-dataflow-composed-task-runner" "spring-cloud-dataflow-single-step-batch-job")
for app in ${APPS[@]}; do
    APP_PATH="$ROOT_DIR/$app/target"
    if [ ! -f "$APP_PATH/$app-$TAG.jar" ]; then
        echo "Cannot find $APP_PATH/$app-$TAG.jar download using download-apps.sh or build using ./mvnw install"
        exit 1
    fi
    jib jar --from=$IMAGE $CRED --target=docker://springcloud/$app:$TAG $APP_PATH/$app-$TAG.jar
    # docker tag springcloud/$app:$TAG springcloud/$app:$ARCH
done
TS_APPS=("spring-cloud-dataflow-tasklauncher-sink-kafka" "spring-cloud-dataflow-tasklauncher-sink-rabbit")
for app in ${TS_APPS[@]}; do
    APP_PATH="$ROOT_DIR/spring-cloud-dataflow-tasklauncher/$app/target"
    if [ ! -f "$APP_PATH/$app-$TAG.jar" ]; then
        echo "Cannot find $APP_PATH/$app-$TAG.jar download using download-apps.sh or build using ./mvnw install"
        exit 1
    fi
    jib jar --from=$IMAGE $CRED --target=docker://springcloud/$app:$TAG $APP_PATH/$app-$TAG.jar
done

pushd $ROOT_DIR >/dev/null
docker build -t springcloud/spring-cloud-dataflow-prometheus-local:$TAG src/grafana/prometheus/docker/prometheus-local
popd >/dev/null
