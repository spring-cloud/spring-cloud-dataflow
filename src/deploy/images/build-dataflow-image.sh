#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOTDIR=$(realpath "$SCDIR/../../..")
$ROOTDIR/mvnw -o -am -pl :spring-cloud-dataflow-server install -DskipTests -T 0.5C
$ROOTDIR/mvnw -o -pl :spring-cloud-dataflow-server spring-boot:build-image -DskipTests
