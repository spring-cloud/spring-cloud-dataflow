#!/usr/bin/env bash
export DATAFLOW_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
export SKIPPER_VERSION=$(mvn help:evaluate -Dexpression=spring-cloud-skipper.version  -pl spring-cloud-dataflow-parent -q -DforceStdout)
export DATAFLOW_VERSION_NOPOSTFIX=$(echo '${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion}' | mvn build-helper:parse-version help:evaluate -q -DforceStdout)

echo "DATAFLOW_VERSION=$DATAFLOW_VERSION"
echo "SKIPPER_VERSION=$SKIPPER_VERSION"
echo "DATAFLOW_VERSION_NOPOSTFIX=$DATAFLOW_VERSION_NOPOSTFIX"