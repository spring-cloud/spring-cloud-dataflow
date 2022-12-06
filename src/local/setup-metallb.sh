#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

echo "Deploying Metal LoadBalancer"
sh "$SCDIR/load-image.sh" "quay.io/metallb/speaker" "v0.12.1"
sh "$SCDIR/load-image.sh" "quay.io/metallb/controller" "v0.12.1"

kubectl apply -f "$SCDIR/k8s/metallb-ns.yaml"
kubectl apply -f "$SCDIR/k8s/metallb.yaml"

kubectl rollout status ds speaker --namespace=metallb-system
kubectl rollout status deploy controller --namespace=metallb-system

docker network inspect -f '{{.IPAM.Config}}' kind
echo "Modify $SCDIR/k8s/metallb-configmap.yaml to use the address range."
