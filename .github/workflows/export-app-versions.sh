#!/usr/bin/env bash
DATAFLOW_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
SKIPPER_VERSION=$(mvn help:evaluate -Dexpression=spring-cloud-skipper.version  -pl spring-cloud-dataflow-parent -q -DforceStdout)
if [[ "$SKIPPER_VERSION" = *"ERROR"* ]]; then
    SKIPPER_VERSION=$(mvn help:evaluate -Dexpression=spring-cloud-skipper.version -q -DforceStdout)
fi

DATAFLOW_VERSION_NOPOSTFIX=$(echo '${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion}' | mvn build-helper:parse-version help:evaluate -q -DforceStdout)

export DATAFLOW_VERSION
export SKIPPER_VERSION
export DATAFLOW_VERSION_NOPOSTFIX

echo "DATAFLOW_VERSION=$DATAFLOW_VERSION"
echo "SKIPPER_VERSION=$SKIPPER_VERSION"
echo "DATAFLOW_VERSION_NOPOSTFIX=$DATAFLOW_VERSION_NOPOSTFIX"