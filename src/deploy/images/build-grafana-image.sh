#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
PROJECT_DIR=$(realpath $SCDIR/../../..)
pushd $PROJECT_DIR || exit
    if [ "$DATAFLOW_VERSION" = "" ]; then
        SCDF_VER=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    else
        SCDF_VER=$DATAFLOW_VERSION
    docker build -f "src/grafana/prometheus/docker/grafana" "springcloud/spring-cloud-dataflow-grafana-prometheus:$SCDF_VER"
popd || exit
