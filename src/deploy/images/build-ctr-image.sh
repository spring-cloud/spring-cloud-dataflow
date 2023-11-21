#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
$SCDIR/mvnw -o -am -pl :spring-cloud-dataflow-composed-task-runner install -DskipTests -T 0.5C
$SCDIR/mvnw -o -pl :spring-cloud-dataflow-composed-task-runner spring-boot:build-image -DskipTests
