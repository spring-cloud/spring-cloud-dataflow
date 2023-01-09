#!/bin/bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
export K8S_DRIVER=kind
export KUBECONFIG=
if [ "$1" = "" ]; then
  export NS=default
else
  export NS=$1
fi
echo "Namespace: $NS"
