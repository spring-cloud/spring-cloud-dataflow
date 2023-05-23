#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

VERSION=$(yq '.scdf-type.oss.release' "$SCDIR/../versions.yaml")
SHELL_JAR="$SCDIR/spring-cloud-dataflow-shell-$VERSION.jar"
if [ ! -f "$SHELL_JAR" ]; then
    echo "Downloading $SHELL_JAR"
    curl -o "$SHELL_JAR" "https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dataflow-shell/$VERSION/spring-cloud-dataflow-shell-$VERSION.jar"
    echo "Downloaded $SHELL_JAR"
fi
if [ "$DATAFLOW_URL" != "" ]; then
    ARGS="--dataflow.uri=$DATAFLOW_URL"
fi
java -jar "$SHELL_JAR" $ARGS $*
