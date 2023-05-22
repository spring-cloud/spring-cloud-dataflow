#!/usr/bin/env bash
set +e
DATAFLOW_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
RC=$?
if ((RC!=0)); then
    echo "DATAFLOW_VERSION=$DATAFLOW_VERSION"
    exit $RC
fi
SKIPPER_VERSION=$(mvn help:evaluate -Dexpression=spring-cloud-skipper.version  -pl spring-cloud-dataflow-parent -q -DforceStdout)
if [[ "$SKIPPER_VERSION" = *"ERROR"* ]]; then
    SKIPPER_VERSION=$(mvn help:evaluate -Dexpression=spring-cloud-skipper.version -q -DforceStdout)
fi
RC=$?
if ((RC!=0)); then
    echo "SKIPPER_VERSION=$SKIPPER_VERSION"
    exit $RC
fi
DATAFLOW_VERSION_NOPOSTFIX=$(echo '${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion}' | mvn build-helper:parse-version help:evaluate -q -DforceStdout)
RC=$?
if ((RC!=0)); then
    echo "DATAFLOW_VERSION_NOPOSTFIX=$DATAFLOW_VERSION_NOPOSTFIX"
    exit $RC
fi
export DATAFLOW_VERSION
export SKIPPER_VERSION
export DATAFLOW_VERSION_NOPOSTFIX