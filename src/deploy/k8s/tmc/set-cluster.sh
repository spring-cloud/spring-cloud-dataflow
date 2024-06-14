#!/usr/bin/env bash
# export TMC_CLUSTER=scdf-manual-empty-1654874129329054695
if [ "$TMC_CLUSTER" = "" ]; then
  echo "Configure TMC_CLUSTER with the cluster name" >&2
  exit 1
fi
tmc cluster auth kubeconfig get $TMC_CLUSTER
RC=$?
if [ "$RC" != "0" ]; then
  echo "Cannot connect to $TMC_CLUSTER" >&2
  exit 1
fi
tmc cluster auth kubeconfig get $TMC_CLUSTER >/tmp/kubeconfig
export KUBECONFIG=/tmp/kubeconfig
