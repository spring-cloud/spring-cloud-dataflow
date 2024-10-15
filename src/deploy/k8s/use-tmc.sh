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
export K8S_DRIVER=tmc
if [ "$1" = "" ]; then
  echo "Expected parameter providing TMC cluster"
  return 2
fi
export TMC_CLUSTER="$1"
echo "Connecting to $TMC_CLUSTER"
tmc cluster auth kubeconfig get $TMC_CLUSTER > $HOME/.kube/config
export KUBECONFIG=$HOME/.kube/config
shift
source $SCDIR/set-ver.sh $*
