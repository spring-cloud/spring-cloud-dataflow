#!/usr/bin/env bash
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
if [ "$1" = "" ]; then
  echo "Arguments <driver> [--release | --snapshot | --milestone] [--pro] [--skip-reg] [--namespace <namespace> | -ns <namespace>] [postgres | postgresql | maria | mariadb | mysql | mysql57] [rabbit | rabbitmq | kafka] [prometheus | grafana]"
  echo "Driver must be one of kind, Or a valid driver for minikube like kvm2, docker, vmware, virtualbox, podman, vmwarefusion, hyperkit"
  return 0
fi

K8S_DRIVER=$1
export K8S_DRIVER
KUBECONFIG=
export KUBECONFIG
shift
source $SCDIR/set-ver.sh $*

