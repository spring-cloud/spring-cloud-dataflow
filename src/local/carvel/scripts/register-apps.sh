#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
function register_app() {
  set +e
  echo "Registering $1 as $2"
  wget -q -O- "$DATAFLOW_URL/apps/$1" --post-data="uri=$2"
  RC=$?
  if ((RC > 0)); then
    echo "Error registering $1: $RC"
  fi
}
if  [ "$1" = "" ]; then
  echo "Arguments: <broker> [stream-applications-version]"
  echo "  broker: Should be one of rabbitmq or kafka"
  echo "  stream-applications-version: Optional. Use 2021.1.2 for latest release."
  exit 1
fi

if [ "$2" != "" ]; then
  STREAM_APPS_VERSION=$2
fi
if [ "$BROKER" == "kafka" ]; then
  BROKER_NAME=kafka
else
  # unfortunately different in docker image names and registration link.
  BROKER_NAME=rabbit
fi  

if [ "$STREAM_APPS_VERSION" = "" ]; then
  STREAM_URI="https://dataflow.spring.io/$BROKER-docker-latest"
else
  STREAM_APPS_VERSION=2021.1.2
  STREAM_URI=https://repo.maven.apache.org/maven2/org/springframework/cloud/stream/app/stream-applications-descriptor/$STREAM_APPS_VERSION/stream-applications-descriptor-$STREAM_APPS_VERSION.stream-apps-$BROKER-docker
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

TASK_URI=https://dataflow.spring.io/task-docker-latest
echo "Registering Task applications at $DATAFLOW_URL using $TASK_URI"
wget -qO- "$DATAFLOW_URL/apps" --post-data="uri=$TASK_URI"

# replace with individual calls to register only what is required.
#register_app "task/timestamp" "docker:springcloudtask/timestamp-task:2.0.2"
#register_app "task/timestamp-batch" "docker:springcloudtask/timestamp-batch-task:2.0.2"
