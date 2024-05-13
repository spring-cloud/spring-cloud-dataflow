#!/bin/bash
if [ -n "$BASH_SOURCE" ]; then
  SCDIR="$(readlink -f "${BASH_SOURCE[0]}")"
elif [ -n "$ZSH_VERSION" ]; then
  setopt function_argzero
  SCDIR="${(%):-%N}"
elif eval '[[ -n ${.sh.file} ]]' 2>/dev/null; then
  eval 'SCDIR=${.sh.file}'
else
  echo 1>&2 "Unsupported shell. Please use bash, ksh93 or zsh."
  exit 2
fi
SCDIR="$(dirname "$SCDIR")"

PROJECT_DIR=$(realpath "$SCDIR/../../..")
pushd "$PROJECT_DIR" > /dev/null || exit
    if [ "$DATAFLOW_VERSION" = "" ]; then
        SCDF_VER=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    else
        SCDF_VER=$DATAFLOW_VERSION
    fi
    pushd src/grafana/prometheus/docker/grafana || exit 1
        docker build -t "springcloud/spring-cloud-dataflow-grafana-prometheus:$SCDF_VER" .
    popd || exit 1
popd || exit
