#!/bin/bash
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

(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
if [ "$1" = "" ]; then
  echo "Arguments <driver> [--namespace <namespace>] [database] [broker] [--pro] [--skip-reg] [--release] [--snapshot] [--milestone]"
  echo "Driver must be one of kind, Or a valid driver for minikube like kvm2, docker, vmware, virtualbox, podman, vmwarefusion, hyperkit"
  return 0
fi

K8S_DRIVER=$1
export K8S_DRIVER
KUBECONFIG=
export KUBECONFIG
shift
source "$SCDIR/set-ver.sh"

