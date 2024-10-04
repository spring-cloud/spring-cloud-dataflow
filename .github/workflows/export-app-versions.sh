#!/usr/bin/env bash
set +e
if [ "$MVN" = "" ]; then
    MVN=./mvnw
fi
$MVN --version
$MVN help:evaluate -Dexpression=project.version > /dev/null
DATAFLOW_VERSION=$($MVN help:evaluate -Dexpression=project.version -q -DforceStdout)
RC=$?
if ((RC!=0)); then
    echo "DATAFLOW_VERSION=$DATAFLOW_VERSION"
    exit $RC
fi
echo "DATAFLOW_VERSION=$DATAFLOW_VERSION"
SKIPPER_VERSION=$($MVN help:evaluate -Dexpression=spring-cloud-skipper.version  -pl spring-cloud-dataflow-parent -q -DforceStdout)
if [[ "$SKIPPER_VERSION" = *"ERROR"* ]]; then
    SKIPPER_VERSION=$($MVN help:evaluate -Dexpression=spring-cloud-skipper.version -q -DforceStdout)
fi
RC=$?
if ((RC!=0)); then
    echo "SKIPPER_VERSION=$SKIPPER_VERSION"
    exit $RC
fi
echo "SKIPPER_VERSION=$SKIPPER_VERSION"
export DATAFLOW_VERSION
export SKIPPER_VERSION
