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

kubectl_pid=$(ps aux | grep 'kubectl' | grep 'port\-forward' | awk '{print $2}')
if [ "$kubectl_pid" != "" ]
then
  kill $kubectl_pid
fi
if [ "$K8S_DRIVER" = "" ]; then
  K8S_DRIVER=kind
fi

case "$K8S_DRIVER" in
"kind")
  kind delete cluster
  ;;
"gke")
  sh "$SCDIR/delete-scdf.sh"
  sh "$SCDIR/tmc/delete-cluster.sh"
  ;;
"tmc")
  sh "$SCDIR/delete-scdf.sh"
  sh "$SCDIR/tmc/delete-cluster.sh"
  ;;
*)
  minikube delete
  ;;
esac
