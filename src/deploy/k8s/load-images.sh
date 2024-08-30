#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
case $BROKER in
"" | "kafka")
  export BROKER=kafka
  export BROKER_NAME=kafka
  ;;
"rabbit" | "rabbitmq")
  export BROKER=rabbitmq
  export BROKER_NAME=rabbit
  ;;
*)
  echo "BROKER=$BROKER not supported"
esac
if [ "$STREAM_APPS_VERSION" == "" ]; then
    if [[ "$DATAFLOW_VERSION" == *"SNAPSHOT"* ]]; then
        STREAM_APPS_VERSION="5.0.1-SNAPSHOT"
    else
        STREAM_APPS_VERSION="5.0.0"
    fi
fi

if [ "$BROKER" = "rabbitmq" ]; then
  # Sample Stream Apps
  echo "Loading Sample Stream Apps images"
  sh "$SCDIR/load-image.sh" "springcloudstream/scdf-app-kitchen:1.0.0-SNAPSHOT" true
  sh "$SCDIR/load-image.sh" "springcloudstream/scdf-app-customer:1.0.0-SNAPSHOT" true
  sh "$SCDIR/load-image.sh" "springcloudstream/scdf-app-waitron:1.0.0-SNAPSHOT" true
fi

# Stream Apps
echo "Loading Stream Apps images"
sh "$SCDIR/load-image.sh" "springcloudstream/log-sink-$BROKER_NAME:$STREAM_APPS_VERSION" false
sh "$SCDIR/load-image.sh" "springcloudstream/http-source-$BROKER_NAME:$STREAM_APPS_VERSION" false
sh "$SCDIR/load-image.sh" "springcloudstream/transform-processor-$BROKER_NAME:$STREAM_APPS_VERSION" false
sh "$SCDIR/load-image.sh" "springcloudstream/splitter-processor-$BROKER_NAME:$STREAM_APPS_VERSION" false
sh "$SCDIR/load-image.sh" "springcloudstream/router-sink-$BROKER_NAME:$STREAM_APPS_VERSION" false
sh "$SCDIR/load-image.sh" "springcloudstream/analytics-sink-$BROKER_NAME:$STREAM_APPS_VERSION" false
sh "$SCDIR/load-image.sh" "springcloudstream/time-source-$BROKER_NAME:$STREAM_APPS_VERSION" false

# Task Apps
echo "Loading Task Apps images"
sh "$SCDIR/load-image.sh" "springcloudtask/timestamp-task:2.0.2" false
sh "$SCDIR/load-image.sh" "springcloudtask/timestamp-task:3.0.0" false
sh "$SCDIR/load-image.sh" "springcloudtask/timestamp-batch-task:2.0.2" false
sh "$SCDIR/load-image.sh" "springcloudtask/timestamp-batch-task:3.0.0" false
