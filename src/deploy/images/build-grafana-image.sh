#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
PROJECT_DIR=$(realpath $SCDIR/../../..)
pushd $PROJECT_DIR || exit
    if [ "$DATAFLOW_VERSION" = "" ]; then
        ./mvnw help:evaluatev -s .settings.xml -Dexpression=project.version > /dev/null
        SCDF_VER=$(./mvnw help:evaluate -Dexpression=project.version -o -q -DforceStdout)
    else
        SCDF_VER=$DATAFLOW_VERSION
    fi
    pushd src/grafana/prometheus/docker/grafana || exit 1
        docker build -t "springcloud/spring-cloud-dataflow-grafana-prometheus:$SCDF_VER" .
    popd || exit 1
popd || exit
