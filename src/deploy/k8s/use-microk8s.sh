#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
microk8s status
mkdir -p $HOME/.kube
microk8s config > $HOME/.kube/config
export KUBECONFIG=$HOME/.kube/config
echo "KUBECONFIG=$KUBECONFIG"
export K8S_DRIVER=microk8s
source $SCDIR/set-ver.sh $*
