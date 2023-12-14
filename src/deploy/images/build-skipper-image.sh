#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOTDIR=$(realpath "$SCDIR/../../..")
$ROOTDIR/mvnw -T 0.5C -o -am -pl :spring-cloud-skipper-server install -DskipTests
$ROOTDIR/mvnw -o -pl :spring-cloud-skipper-server spring-boot:build-image -DskipTests
