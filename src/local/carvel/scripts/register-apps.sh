#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
if  [ "$1" = "" ]; then
  echo "Arguments: <broker> [stream-applications-version]"
  echo "  broker: Should be one of rabbit or kafka"
  echo "  stream-applications-version: Optional. Use 2021.1.2 for latest release."
  exit 1
fi

if [ "$2" != "" ]; then
  STREAM_APPS_VERSION=$2
fi

if [ "$STREAM_APPS_VERSION" = "" ]; then
  STREAM_URI="https://dataflow.spring.io/$BROKER-docker-latest"
else
  STREAM_APPS_VERSION=2021.1.2
  STREAM_URI=https://repo.maven.org/maven2/org/springframework/cloud/stream/app/stream-applications-descriptor/$STREAM_APPS_VERSION/stream-applications-descriptor-$STREAM_APPS_VERSION.stream-apps-$BROKER-docker
fi
if [ "$DATAFLOW_URL" = "" ]; then
  source $SCDIR/export-dataflow-ip.sh
fi
echo "Registering Stream applications at $DATAFLOW_URL using $STREAM_URI"
wget -qO- $DATAFLOW_URL/apps --post-data="uri=$STREAM_URI"
TASK_URI=https://dataflow.spring.io/task-docker-latest
echo "Registering Task applications at $DATAFLOW_URL using $TASK_URI"
wget -qO- "$DATAFLOW_URL/apps" --post-data="uri=$TASK_URI"
