#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
echo "Deploying Metal LoadBalancer"
sh "$SCDIR/load-image.sh" "quay.io/metallb/speaker:v0.13"
sh "$SCDIR/load-image.sh" "quay.io/metallb/controller:v0.13"

kubectl apply -f "$SCDIR/yaml/metallb-ns.yaml"
kubectl apply -f "$SCDIR/yaml/metallb.yaml"

kubectl rollout status ds speaker --namespace=metallb-system
kubectl rollout status deploy controller --namespace=metallb-system
if [ "$K8S_DRIVER" == "kind" ]; then
    docker network inspect -f '{{.IPAM.Config}}' kind
fi
echo "Modify $SCDIR/yaml/metallb-configmap.yaml to use the address range."
