#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
./mvnw -o -am -pl :spring-cloud-dataflow-single-step-batch-job install -DskipTests -T 0.5C
./mvnw -o -pl :spring-cloud-dataflow-single-step-batch-job spring-boot:build-image -DskipTests
