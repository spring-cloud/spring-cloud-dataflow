#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
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
