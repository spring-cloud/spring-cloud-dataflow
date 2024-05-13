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



set -e
echo "Waiting for dataflow"
if [ "$K8S_DRIVER" = "" ]; then
  K8S_DRIVER=kind
fi
if [ "$USE_PRO" = "true" ]; then
  kubectl rollout status deployment --namespace "$NS" scdf-spring-cloud-dataflow-server
else
  kubectl rollout status deployment --namespace "$NS" scdf-server
fi
kubectl_pid=$(ps aux | grep 'kubectl' | grep 'port\-forward' | awk '{print $2}')
if [ "$kubectl_pid" != "" ]
then
  kill $kubectl_pid
fi
kubectl_pid=$(ps aux | grep 'kubectl' | grep 'port\-forward' | awk '{print $2}')
if [ "$kubectl_pid" != "" ]
then
  kill $kubectl_pid
fi
kubectl port-forward --namespace "$NS" svc/scdf-server "9393:9393" &
if [ "$PROMETHEUS" = "true" ]; then
  kubectl port-forward --namespace "$NS" svc/grafana "3000:3000" &
fi

export DATAFLOW_IP="http://localhost:9393"
echo "DATAFLOW_IP=$DATAFLOW_IP"
