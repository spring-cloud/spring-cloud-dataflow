#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath "$SCDIR/../..")
pushd "$SCDIR" || exit
    ./mvnw install -DskipTests -Pdocs -B --no-transfer-progress -pl :spring-cloud-skipper-server-core,:spring-cloud-skipper,:spring-cloud-skipper-docs,:spring-cloud-dataflow-classic-docs,:spring-cloud-dataflow-docs -T 1C
popd > /dev/null || exit
