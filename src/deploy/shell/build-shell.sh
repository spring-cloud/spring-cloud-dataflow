#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
./mvnw -o -am -pl :spring-cloud-dataflow-shell install -DskipTests -T 0.5C
DATAFLOW_VERSION=$(./mvnw exec:exec -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive -q | sed 's/\"//g' | sed 's/version=//g')
SRC="./spring-cloud-dataflow-shell/target/spring-cloud-dataflow-shell-${DATAFLOW_VERSION}.jar"
if [ ! -f "$SRC" ]; then
    echo "Cannot find $SRC"
fi
echo "Built $SRC"
cp "$SRC" ./src/deploy/shell/
echo "Copied: $SRC"
echo "set DATAFLOW_VERSION=$DATAFLOW_VERSION to use this version of the shell"
