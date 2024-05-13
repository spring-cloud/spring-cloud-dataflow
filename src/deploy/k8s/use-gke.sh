#!/bin/bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
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

if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi

function usage() {
    echo "Usage $0 <cluster> [namespace]"
}
export K8S_DRIVER=gke
export GKE_CLUSTER="$1"
echo "Connecting to $GKE_CLUSTER..."
export REGION=$(gcloud container clusters list | grep -F "$CLUSTER_NAME" | awk '{print $2}')
if [ "$REGION" = "" ]; then
  echo "Cannot find REGION from $CLUSTER_NAME"
  return 1
fi
print "\rConnecting to $GKE_CLUSTER at $REGION"
export KUBECONFIG=$HOME/.kube/config-gke
gcloud container clusters get-credentials $GKE_CLUSTER --region $REGION
echo "KUBECONFIG set to $KUBECONFIG"
shift
if [ "$1" != "" ]; then
  export NS=$1
  shift
fi
if [ "$NS" = "" ]; then
  export NS=scdf
fi
echo "Namespace: $NS"
source $SCDIR/set-ver.sh

