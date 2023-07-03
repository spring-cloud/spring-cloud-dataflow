#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOTDIR=$(realpath $(pwd))
./mvnw -o -am -pl :spring-cloud-starter-dataflow-server install -DskipTests
pushd "$ROOTDIR/../scdf-pro"  > /dev/null || exit
    $ROOTDIR/mvnw -o -am -pl :scdf-pro-server clean install -DskipTests
    $ROOTDIR/mvnw -o -pl :scdf-pro-server spring-boot:build-image -DskipTests
popd > /dev/null || exit
