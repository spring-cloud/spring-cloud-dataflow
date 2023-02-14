#!/bin/bash

bold="\033[1m"
dim="\033[2m"
end="\033[0m"

if [ "$NS" = "" ]; then
  echo "NS not defined" >&2
  exit 2
fi
start_time=$(date +%s)
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
K8S_PATH=$(realpath $SCDIR/k8s)
set -e
if [ "$K8S_DRIVER" = "" ]; then
  K8S_DRIVER=kind
fi
K8S_VERSION=1.24.10
case "$K8S_DRIVER" in
"kind")
  echo "Creating kind cluster"
  kind create cluster --image "kindest/node:v$K8S_VERSION"
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
*)
  echo "Creating Minikube cluster with $K8S_DRIVER and k8s=$K8S_K8S_VERSION"
  # K8S_DRIVER=kvm2, docker, vmware, virtualbox, podman, vmwarefusion or hyperkit
  minikube start --cpus=8 --memory=16g "--driver=$K8S_DRIVER" "--kubernetes-version=$K8S_VERSION"
  echo -e "Please run ${bold}'minikube tunnel'${end} in a separate shell to ensure a LoadBalancer is active."
  ;;
esac
COUNT=$(kubectl get namespaces | grep -c "$NS")
if ((COUNT == 0)); then
  kubectl create namespace "$NS"
fi
if [ "$K8S_DRIVER" = "kind" ]; then
  sh "$SCDIR/setup-metallb.sh"
fi
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Kubernetes on $K8S_DRIVER running in ${bold}$elapsed${end} seconds"
