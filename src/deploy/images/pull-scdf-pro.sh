#!/bin/bash
if [ "$DATAFLOW_PRO_VERSION" = "" ]; then
  DATAFLOW_PRO_VERSION=1.6.1-SNAPSHOT
fi
docker pull "spring-scdf-docker-dev-local.usw1.packages.broadcom.com/p-scdf-for-kubernetes/scdf-pro-server:$DATAFLOW_PRO_VERSION"
