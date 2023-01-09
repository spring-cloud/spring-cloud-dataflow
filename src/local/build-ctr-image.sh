#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOTDIR=$(realpath "$SCDIR/../..")
if [ "$DATAFLOW_VERSION" = "" ]; then
  DATAFLOW_VERSION=2.10.1-SNAPSHOT
fi
pushd $ROOTDIR  > /dev/null
    ./mvnw -o clean install -DskipTests
    pushd spring-cloud-dataflow-composed-task-runner  > /dev/null
        ../mvnw -o spring-boot:build-image -DskipTests -Dspring-boot.build-image.imageName=springcloud/spring-cloud-dataflow-composed-task-runner:$DATAFLOW_VERSION
    popd > /dev/null
popd > /dev/null
