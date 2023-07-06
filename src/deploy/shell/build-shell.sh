#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
./mvnw -o -am -pl :spring-cloud-dataflow-shell install -DskipTests -T 0.5C
DATAFLOW_VERSION=$(./mvnw exec:exec -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive -q | sed 's/\"//g' | sed 's/version=//g')
cp ./spring-cloud-dataflow-shell/target/spring-cloud-dataflow-shell-${DATAFLOW_VERSION}.jar ./src/deploy/shell/
echo "set DATAFLOW_VERSION=$DATAFLOW_VERSION to use this version of the shell"
