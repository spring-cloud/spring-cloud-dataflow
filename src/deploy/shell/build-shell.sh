#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
./mvnw -o -am -pl :spring-cloud-dataflow-shell install -DskipTests -T 1C  -B --no-transfer-progress
DATAFLOW_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
SRC="./spring-cloud-dataflow-shell/target/spring-cloud-dataflow-shell-${DATAFLOW_VERSION}.jar"
if [ ! -f "$SRC" ]; then
    echo "Cannot find $SRC"
fi
echo "Built $SRC"
cp "$SRC" ./src/deploy/shell/
echo "Copied: $SRC"
echo "set DATAFLOW_VERSION=$DATAFLOW_VERSION to use this version of the shell"
