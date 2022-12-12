#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOTDIR=$(realpath "$SCDIR/../..")
if [ "$SKIPPER_VERSION" = "" ]; then
  SKIPPER_VERSION=2.9.1-SNAPSHOT
fi

pushd "$ROOTDIR/../spring-cloud-skipper" > /dev/null
    ./mvnw -o clean install -DskipTests
    pushd spring-cloud-skipper-server > /dev/null
        ../mvnw -o spring-boot:build-image -DskipTests -Dspring-boot.build-image.imageName=springcloud/spring-cloud-skipper-server:$SKIPPER_VERSION
    popd > /dev/null
popd > /dev/null
