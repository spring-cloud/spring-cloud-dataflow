#!/bin/bash
if [ "$DATAFLOW_PRO_VERSION" = "" ]; then
  DATAFLOW_PRO_VERSION=1.5.0-SNAPSHOT
fi
docker pull "dev.registry.pivotal.io/p-scdf-for-kubernetes/scdf-pro-repo/scdfpro.tanzu.vmware.com:$DATAFLOW_PRO_VERSION"
