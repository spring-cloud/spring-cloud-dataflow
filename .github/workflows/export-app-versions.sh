#!/usr/bin/env bash
export SPRING_CLOUD_DATAFLOW_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
export SPRING_CLOUD_SKIPPER_VERSION=$(mvn help:evaluate -Dexpression=spring-cloud-skipper.version  -pl spring-cloud-dataflow-parent -q -DforceStdout)
export SPRING_CLOUD_DATAFLOW_VERSION_NOPOSTFIX=$(echo '${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion}' | mvn build-helper:parse-version help:evaluate -q -DforceStdout)

echo "SPRING_CLOUD_DATAFLOW_VERSION=$SPRING_CLOUD_DATAFLOW_VERSION"
echo "SPRING_CLOUD_SKIPPER_VERSION=$SPRING_CLOUD_SKIPPER_VERSION"
echo "SPRING_CLOUD_DATAFLOW_VERSION_NOPOSTFIX=$SPRING_CLOUD_DATAFLOW_VERSION_NOPOSTFIX"