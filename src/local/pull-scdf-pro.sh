#!/bin/bash
if [ "$DATAFLOW_PRO_VERSION" = "" ]; then
  DATAFLOW_PRO_VERSION=1.5.3-SNAPSHOT
fi
docker pull "dev.registry.pivotal.io/p-scdf-for-kubernetes/scdf-pro-server:$DATAFLOW_PRO_VERSION"
