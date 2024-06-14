#!/usr/bin/env bash
if [ "$TMC_CLUSTER" = "" ]; then
  echo "TMC_CLUSTER environmental variable not found" >&2
  exit 2
fi
if [ "$NS" = "" ]; then
  echo "NS not defined" >&2
  exit 2
fi
echo "Delete all pods in $NS"
kubectl delete pod --namespace "$NS" --all
echo "Delete all volumes in $NS"
kubectl delete pvc --namespace "$NS" --all
echo "Delete cluster $TMC_CLUSTER"
tmc cluster delete "$TMC_CLUSTER"
