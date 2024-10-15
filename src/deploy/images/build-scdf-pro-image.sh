#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOTDIR=$(realpath "$SCDIR/../../..")
./mvnw -o -am -pl :spring-cloud-starter-dataflow-server install -DskipTests  -B --no-transfer-progress
pushd "$ROOTDIR/../scdf-pro"  > /dev/null || exit
    ./mvnw -o -am -pl :scdf-pro-server clean install -DskipTests  -B --no-transfer-progress
    ./mvnw -o -pl :scdf-pro-server spring-boot:build-image -DskipTests  -B --no-transfer-progress
popd > /dev/null || exit
