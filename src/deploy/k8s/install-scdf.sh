#!/bin/bash
if [ "$NS" = "" ]; then
  echo "NS not defined" >&2
  exit 2
fi
start_time=$(date +%s)
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
LS_DIR=$(realpath $SCDIR)
K8S_PATH="$LS_DIR/yaml"
set -e
if [ "$K8S_DRIVER" = "" ]; then
  K8S_DRIVER=kind
fi
if [ "$BROKER" = "" ]; then
  export BROKER="kafka"
fi
export PLATFORM_TYPE=kubernetes
if [ "$K8S_DRIVER" = "kind" ]; then
  kubectl apply -f "$K8S_PATH/metallb-configmap.yaml"
fi

sh "$LS_DIR/deploy-scdf.sh"

if [ "$K8S_DRIVER" != "tmc" ]; then
  sh "$LS_DIR/load-images.sh"
fi

if [ "$DATABASE" = "mariadb" ]; then
    echo "Waiting for mariadb"
    kubectl rollout status deployment --namespace "$NS" mariadb
else
    echo "Waiting for PostgreSQL"
    kubectl rollout status deployment --namespace "$NS" postgresql
fi

if [ "$BROKER" = "kafka" ]; then
  echo "Waiting for Kafka and Zookeeper"
  kubectl rollout status deployment --namespace "$NS" kafka-zk
  kubectl rollout status sts --namespace "$NS" kafka-broker
else
  echo "Waiting for rabbitmq"
  kubectl rollout status deployment --namespace "$NS" rabbitmq
fi
echo "Waiting for skipper"
kubectl rollout status deployment --namespace "$NS" skipper
echo "Waiting for dataflow"
kubectl rollout status deployment --namespace "$NS" scdf-server

if [ "$K8S_DRIVER" = "kind" ]; then
  source "$LS_DIR/forward-scdf.sh"
  # waiting for port-forwarding to be active
  sleep 2
else
  source "$LS_DIR/export-dataflow-ip.sh"
fi
sh "$LS_DIR/register-apps.sh"
end_time=$(date +%s)
elapsed=$(( end_time - start_time ))
echo "Complete deployment in $elapsed seconds"
echo "Execute source $LS_DIR/export-dataflow-ip.sh to export DATAFLOW_IP for executing tests"
echo "Monitor pods using k9s and kail --ns=default | tee pods.log"
