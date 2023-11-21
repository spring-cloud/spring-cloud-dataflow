#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOTDIR=$(realpath $(pwd))
./mvnw -o -am -pl :spring-cloud-starter-dataflow-server install -DskipTests
pushd "$ROOTDIR/../scdf-pro"  > /dev/null || exit
    ./mvnw -o -am -pl :scdf-pro-skipper clean install -DskipTests
    ./mvnw -o -pl :scdf-pro-skipper spring-boot:build-image -DskipTests
popd > /dev/null || exit
