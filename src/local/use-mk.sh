#!/bin/bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
if [ "$1" = "" ]; then
  echo "Expected parameter providing MiniKube driver type."
  echo "Choose one of kvm2, docker, vmware, virtualbox, podman, vmwarefusion, hyperkit"
  return 2
fi
export K8S_DRIVER=$1
export KUBECONFIG=
if [ "$2" = "" ]; then
  export NS=default
else
  export NS=$2
fi
echo "Namespace: $NS"
