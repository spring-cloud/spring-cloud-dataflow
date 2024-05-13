#!/usr/bin/env bash
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

if [ "$DATAFLOW_VERSION" = "" ]; then
    SCDF_TYPE=$(yq '.default.scdf-type' "$SCDIR/../versions.yaml")
    VER_TYPE=$(yq '.default.version'  "$SCDIR/../versions.yaml")
    DATAFLOW_VERSION=$(yq ".scdf-type.$SCDF_TYPE.$VER_TYPE" "$SCDIR/../versions.yaml")
fi
SHELL_JAR="$SCDIR/spring-cloud-dataflow-shell-$DATAFLOW_VERSION.jar"
if [ ! -f "$SHELL_JAR" ]; then
    echo "Downloading $SHELL_JAR"
    if [[ "$DATAFLOW_VERSION" == *"SNAPSHOT"* ]]; then
        URL="https://repo.spring.io/artifactory/snapshot/org/springframework/cloud/spring-cloud-dataflow-shell/$DATAFLOW_VERSION/spring-cloud-dataflow-shell-$DATAFLOW_VERSION.jar"
        echo "Please visit $URL to download the latest jar file and save as $SHELL_JAR"
        exit 1
    elif [[ "$DATAFLOW_VERSION" == *"-M"* ]] || [[ "$DATAFLOW_VERSION" == *"-RC"* ]]; then
        URL="https://repo.spring.io/artifactory/milestone/org/springframework/cloud/spring-cloud-dataflow-shell/$DATAFLOW_VERSION/spring-cloud-dataflow-shell-$DATAFLOW_VERSION.jar"
        echo "Downloading $URL"
        set -e
        curl -o "$SHELL_JAR" "$URL"
        echo "Downloaded $SHELL_JAR"
    else
        URL="https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dataflow-shell/$DATAFLOW_VERSION/spring-cloud-dataflow-shell-$DATAFLOW_VERSION.jar"
        set -e
        echo "Downloading $URL"
        curl -o "$SHELL_JAR" "$URL"
        echo "Downloaded $SHELL_JAR"
    fi
fi
if [ "$DATAFLOW_URL" != "" ]; then
    ARGS="--dataflow.uri=$DATAFLOW_URL"
fi
java -jar "$SHELL_JAR" $ARGS $*
