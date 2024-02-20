#!/bin/bash
bold="\033[1m"
dim="\033[2m"
end="\033[0m"
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
start_time=$(date +%s)
function register_app() {
    set +e
    echo "Registering $1 as $2"
    DATA="uri=$2"
    if [ "$3" != "" ]; then
        DATA="$DATA&$3"
    fi
    wget -q -O- "$DATAFLOW_URL/apps/$1" --post-data="$DATA"
    RC=$?
    if ((RC > 0)); then
        echo "Error registering $1: $RC"
    fi
}
if [ "$BROKER" = "" ]; then
    echo "BROKER must be defined"
    exit 1
fi
if [ "$1" = "" ]; then
    echo "Arguments: [stream-applications-version] [type]"
    echo "  stream-applications-version: Optional. Use 2021.1.2 for latest release."
    echo "  type: docker or maven"
fi
if [ "$TYPE" = "" ]; then
    TYPE=docker
fi
if [ "$1" != "" ]; then
    STREAM_APPS_VERSION=$1
fi

if [ "$BROKER" == "kafka" ]; then
    BROKER_NAME=kafka
else
    # unfortunately different in docker image names and registration link.
    BROKER_NAME=rabbitmq
fi

if [ "$STREAM_APPS_VERSION" = "" ]; then
    STREAM_URI="https://dataflow.spring.io/$BROKER_NAME-${TYPE}-latest"
elif [[ "$STREAM_APPS_VERSION" = *"SNAPSHOT"* ]]; then
    STREAM_APPS_DL_VERSION=$STREAM_APPS_VERSION
    META_DATA="https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/stream-applications-descriptor/${STREAM_APPS_VERSION}/maven-metadata.xml"
    echo "Downloading $META_DATA"
    curl -o maven-metadata.xml -s $META_DATA
    DL_TS=$(xmllint --xpath "/metadata/versioning/snapshot/timestamp/text()" maven-metadata.xml | sed 's/\.//')
    STREAM_APPS_DL_VERSION=$(xmllint --xpath "/metadata/versioning/snapshotVersions/snapshotVersion[extension/text() = 'pom' and updated/text() = '$DL_TS']/value/text()" maven-metadata.xml)
    STREAM_URI="https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/stream-applications-descriptor/${STREAM_APPS_VERSION}/stream-applications-descriptor-${STREAM_APPS_DL_VERSION}.stream-apps-${BROKER_NAME}-${TYPE}"
else
    REL_TYPE=
    if [[ "$STREAM_APPS_VERSION" = *"-M"* ]] || [[ "$STREAM_APPS_VERSION" = *"-RC"* ]]; then
        REL_TYPE=milestone
    fi
    if [ "$REL_TYPE" != "" ]; then
        STREAM_URI="https://repo.spring.io/$REL_TYPE/org/springframework/cloud/stream/app/stream-applications-descriptor/${STREAM_APPS_VERSION}/stream-applications-descriptor-${STREAM_APPS_VERSION}.stream-apps-${BROKER_NAME}-${TYPE}"
    else
        STREAM_URI="https://repo.maven.apache.org/maven2/org/springframework/cloud/stream/app/stream-applications-descriptor/${STREAM_APPS_VERSION}/stream-applications-descriptor-${STREAM_APPS_VERSION}.stream-apps-${BROKER_NAME}-${TYPE}"
    fi
fi
if [ "$DATAFLOW_URL" = "" ]; then
    source $SCDIR/export-dataflow-ip.sh
fi

echo "Registering Stream applications at $DATAFLOW_URL using $STREAM_URI"
wget -qO- $DATAFLOW_URL/apps --post-data="uri=$STREAM_URI"

# replace with individual invocations of register_app for only those applications used.
#register_app "source/file" "docker:springcloudstream/file-source-$BROKER_NAME:3.2.1"
#register_app "source/ftp" "docker:springcloudstream/ftp-source-$BROKER_NAME:3.2.1"
#register_app "processor/aggregator" "docker:springcloudstream/aggregator-processor-$BROKER_NAME:3.2.1"
#register_app "processor/filter" "docker:springcloudstream/filter-processor-$BROKER_NAME:3.2.1"
#register_app "processor/groovy" "docker:springcloudstream/groovy-processor-$BROKER_NAME:3.2.1"
#register_app "processor/script" "docker:springcloudstream/script-processor-$BROKER_NAME:3.2.1"
#register_app "processor/splitter" "docker:springcloudstream/splitter-processor-$BROKER_NAME:3.2.1"
#register_app "processor/transform" "docker:springcloudstream/transform-processor-$BROKER_NAME:3.2.1"

TASK_URI=https://dataflow.spring.io/task-${TYPE}-latest
echo "Registering Task applications at $DATAFLOW_URL using $TASK_URI"
wget -qO- "$DATAFLOW_URL/apps" --post-data="uri=$TASK_URI"

# replace with individual calls to register only what is required.
#register_app "task/timestamp" "docker:springcloudtask/timestamp-task:2.0.2"
#register_app "task/timestamp-batch" "docker:springcloudtask/timestamp-batch-task:2.0.2"
register_app "task/timestamp3" "docker:springcloudtask/timestamp-task:3.0.0" "bootVersion=3"
register_app "task/timestamp-batch3" "docker:springcloudtask/timestamp-batch-task:3.0.0" "bootVersion=3"
register_app "task/task-demo-metrics-prometheus" "docker:springcloudtask/task-demo-metrics-prometheus:2.0.1-SNAPSHOT"

end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Registered apps from $STREAM_URI in ${bold}$elapsed${end} seconds"
