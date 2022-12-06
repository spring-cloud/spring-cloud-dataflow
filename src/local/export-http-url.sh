#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
PARENT=$(realpath "$SCDIR/../../..")
if [ "$DATAFLOW_IP" = "" ]; then
  source $SCDIR/export-dataflow-ip.sh
fi
if [ "$1" = "" ]; then
  echo "Name of stream is required"
  exit 1
fi
APP=$1
STREAM_JSON=$(curl -s "$DATAFLOW_IP/runtime/streams/$1" | jq -c .)
export HTTP_APP_URL=$(echo "$STREAM_JSON" | jq '._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList | .[] | select(.name == "http") | .instances._embedded.appInstanceStatusResourceList[0].attributes.url' | sed 's/\"//g')
echo "HTTP_APP_URL=$HTTP_APP_URL"
