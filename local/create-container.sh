#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
ROOT_DIR=$(realpath $SCDIR/../..)
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

# set with extra option for buildpacks. BP_OPTIONS=

pack build \
    --path $ROOT_DIR/spring-cloud-skipper-server/target/spring-cloud-skipper-server-$TAG.jar \
    --builder gcr.io/paketo-buildpacks/builder:base \
    --env BP_JVM_VERSION=$v \
    $BP_OPTIONS springcloud/spring-cloud-skipper-server:$TAG
