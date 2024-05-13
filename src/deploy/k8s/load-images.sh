#!/bin/bash
if [ -n "$BASH_SOURCE" ]; then
  SCDIR="$(readlink -f "${BASH_SOURCE[0]}")"
elif [ -n "$ZSH_VERSION" ]; then
  setopt function_argzero
  SCDIR="${(%):-%N}"
elif eval '[[ -n ${.sh.file} ]]' 2>/dev/null; then
  eval 'SCDIR=${.sh.file}'
else
  echo 1>&2 "Unsupported shell. Please use bash, ksh93 or zsh."
    exit 2
fi
SCDIR="$(dirname "$SCDIR")"

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
        STREAM_APPS_VERSION="4.0.0-SNAPSHOT"
    else
        STREAM_APPS_VERSION="4.0.0"
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
