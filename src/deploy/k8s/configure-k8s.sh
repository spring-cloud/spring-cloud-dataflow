#!/usr/bin/env bash

bold="\033[1m"
dim="\033[2m"
end="\033[0m"

if [ "$NS" = "" ]; then
    echo "NS not defined" >&2
    exit 2
fi
start_time=$(date +%s)
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
set -e
if [ "$K8S_DRIVER" = "" ]; then
    K8S_DRIVER=kind
fi
if [ "$1" != "" ]; then
    export K8S_VERSION="$1"
else
    if [ "$K8S_VERSION" == "" ]; then
        export K8S_VERSION="1.30"
    fi
fi
set +e
case "$K8S_DRIVER" in
"kind")
    echo "Creating kind cluster: $K8S_VERSION"
    set -e
    kind create cluster --image "kindest/node:v$K8S_VERSION"
    set +e
    "$SCDIR/setup-metallb.sh"
    ;;
"gke")
    if [ "$GKE_CLUSTER" = "" ]; then
        echo -e "${bold}Cannot find environmental variable GKE_CLUSTER${end}" >&2
        exit 2
    fi
    ;;
"tmc")
    if [ "$TMC_CLUSTER" = "" ]; then
        echo -e "${bold}Cannot find environmental variable TMC_CLUSTER${end}" >&2
        exit 2
    fi
    if [ "$KUBECONFIG" = "" ]; then
        echo "Please execute source $SCDIR/tmc/set-cluster.sh to establish KUBECONFIG" >&2
        exit 2
    fi
    ;;
"microk8s")
    echo "Configure Microk8s"
    microk8s start
    microk8s enable registry
    microk8s enable hostpath-storage
    microk8s enable metallb:172.18.0.1-172.18.0.254
    microk8s kubectl get all --all-namespaces
    microk8s config > $HOME/.kube/config
    ;;
*)
    echo "Creating Minikube cluster with $K8S_DRIVER and k8s=$K8S_VERSION"
    # K8S_DRIVER=kvm2, docker, vmware, virtualbox, podman, vmwarefusion or hyperkit
    if [ "$MK_ARGS" = "" ]; then
        MK_ARGS="--cpus=8 --memory=16g --disk-size=50g"
    fi
    minikube start $MK_ARGS "--driver=$K8S_DRIVER" "--kubernetes-version=$K8S_VERSION"
    echo -e "Please run ${bold}'minikube tunnel'${end} in a separate shell to ensure a LoadBalancer is active."
    ;;
esac
COUNT=$(kubectl get namespaces | grep -c "$NS")
if ((COUNT == 0)); then
    kubectl create namespace "$NS"
fi
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Kubernetes on $K8S_DRIVER running in ${bold}$elapsed${end} seconds"
