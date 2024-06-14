#!/usr/bin/env bash
set -e
if [ "$BROKER" = "rabbitmq" ]; then
    docker pull springcloudstream/scdf-app-kitchen:1.0.0-SNAPSHOT
    docker pull springcloudstream/scdf-app-customer:1.0.0-SNAPSHOT
    docker pull springcloudstream/scdf-app-waitron:1.0.0-SNAPSHOT
fi

echo "Pulling Stream Apps images"
if [ "$BROKER" = "" ]; then
    echo "BROKER not defined"
    exit 1
fi
if [ "$BROKER" = "rabbitmq" ]; then
    BROKER_NAME=rabbit
else
    BROKER_NAME=$BROKER
fi
if [ "$1" != "" ]; then
    STREAM_APPS_VERSION=$1
fi
if [ "$STREAM_APPS_VERSION" = "" ]; then
    STREAM_APPS_VERSION=4.0.0
fi
if [[ "$STREAM_APPS_VERSION" = "202*" ]]; then
    echo "The version expected is not the release train version $STREAM_APPS_VERSION but the apps version."
    exit 1
fi
docker pull "springcloudstream/log-sink-$BROKER_NAME:$STREAM_APPS_VERSION"
docker pull "springcloudstream/http-source-$BROKER_NAME:$STREAM_APPS_VERSION"
docker pull "springcloudstream/transform-processor-$BROKER_NAME:$STREAM_APPS_VERSION"
docker pull "springcloudstream/splitter-processor-$BROKER_NAME:$STREAM_APPS_VERSION"
docker pull "springcloudstream/router-sink-$BROKER_NAME:$STREAM_APPS_VERSION"
docker pull "springcloudstream/analytics-sink-$BROKER_NAME:$STREAM_APPS_VERSION"
docker pull "springcloudstream/time-source-$BROKER_NAME:$STREAM_APPS_VERSION"

echo "Pulling Task Apps images"
docker pull springcloudtask/timestamp-task:2.0.2
docker pull springcloudtask/timestamp-task:3.0.0
docker pull springcloudtask/timestamp-batch-task:2.0.2
docker pull springcloudtask/timestamp-batch-task:3.0.0
