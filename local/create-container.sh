#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
ROOT_DIR=$(realpath $SCDIR/..)
# set to specific version
if [ "$1" != "" ]; then
    TAG=$1
else
    TAG=2.9.0-SNAPSHOT
fi
if [ "$2" != "" ]; then
    v=$2
else
    v=11
fi
# export ARCH=arm64v8 for ARM64 image
if [ "$ARCH" == "" ]; then
    if [ "$HOSTTYPE" == "x86_64" ]; then
        ARCH=amd64
    else
        ARCH=arm64v8
    fi
fi
CRED=
if [ "$DOCKER_USERNAME" != "" ]; then
    CRED="--from-username=$DOCKER_USERNAME --from-password=$DOCKER_PASSWORD"
fi
jib jar --from=$ARCH/eclipse-temurin:$v-jdk-jammy $CRED \
    --target=docker://springcloud/spring-cloud-skipper-server:$TAG \
    $ROOT_DIR/spring-cloud-skipper-server/target/spring-cloud-skipper-server-$TAG.jar
# docker tag springcloud/$app:$TAG springcloud/$app:$ARCH