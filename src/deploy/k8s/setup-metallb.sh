#!/usr/bin/env bash
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

echo "Deploying Metal LoadBalancer"
sh "$SCDIR/load-image.sh" "quay.io/metallb/speaker:v0.13"
sh "$SCDIR/load-image.sh" "quay.io/metallb/controller:v0.13"

kubectl apply -f "$SCDIR/yaml/metallb-ns.yaml"
kubectl apply -f "$SCDIR/yaml/metallb.yaml"

kubectl rollout status ds speaker --namespace=metallb-system
kubectl rollout status deploy controller --namespace=metallb-system
if [ "$K8S_DRIVER" = "kind" ]; then
    docker network inspect -f '{{.IPAM.Config}}' kind
fi
echo "Modify $SCDIR/yaml/metallb-configmap.yaml to use the address range."
