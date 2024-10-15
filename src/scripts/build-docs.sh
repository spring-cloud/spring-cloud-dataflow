#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
pushd "$SCDIR" > /dev/null || exit
    ./mvnw install -DskipTests -am -pl :spring-cloud-dataflow-classic-docs,:spring-cloud-dataflow-docs,:spring-cloud-skipper-server-core,:spring-cloud-skipper-docs -Pfull,docs  -B --no-transfer-progress
popd > /dev/null || exit
