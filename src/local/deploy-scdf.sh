#!/bin/bash
if [ "$NS" = "" ]; then
  echo "NS not defined" >&2
  exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
PARENT=$(realpath "$SCDIR/../..")

case $BROKER in
  "kafka")
    export BROKER="kafka"
    ;;
  "rabbit" | "rabbitmq")
    export BROKER="rabbit"
    ;;
  "")
    export BROKER="kafka"
    ;;
  *)
    echo "BROKER=$BROKER not supported"
    ;;
esac

case $DATABASE in
  "mariadb")
    ;;
  "")
    export DATABASE=mariadb
    ;;
  *)
    echo "DATABASE=$DATABASE not supported"
    ;;
esac

if [ "$K8S_DRIVER" = "" ]; then
  K8S_DRIVER=kind
fi
set +e
COUNT=$(kubectl get namespace | grep -c "$NS")
if [ "$COUNT" = "0" ]; then
  echo "Creating namespace $NS"
  kubectl create namespace "$NS"
else
  echo "Namespace $NS exists"
fi
if [ "$K8S_DRIVER" != "tmc" ] && [ "$K8S_DRIVER" != "gke" ] ; then
  if [ "$DOCKER_USER" = "" ] || [ "$DOCKER_SERVER" = "" ] || [ "$DOCKER_PASSWORD" = "" ]; then
    echo "DOCKER_SERVER, DOCKER_USER, DOCKER_PASSWORD, DOCKER_EMAIL is required" >&2
    exit 1
  fi
  kubectl create secret docker-registry registry-key --namespace "$NS" --docker-server=$DOCKER_SERVER --docker-username=$DOCKER_USER --docker-password=$DOCKER_PASSWORD --docker-email=$DOCKER_EMAIL
  kubectl patch serviceaccount default -p '{"imagePullSecrets": [{"name": "registry-key"}]}'  --namespace "$NS"
fi

if [ "$USE_PRO" = "" ]; then
  USE_PRO=false
fi

if [ "$DATAFLOW_VERSION" = "" ]; then
  DATAFLOW_VERSION=2.10.1-SNAPSHOT
fi

if [ "$SKIPPER_VERSION" = "" ]; then
  SKIPPER_VERSION=2.9.1-SNAPSHOT
fi

if [ "$SCDF_PRO_VERSION" = "" ]; then
  SCDF_PRO_VERSION=1.5.0-SNAPSHOT
fi
K8S_PATH=$(realpath $SCDIR/k8s)

echo "K8S_PATH=$K8S_PATH"

set -e
if [ "$K8S_DRIVER" != "tmc" ] && [ "$K8S_DRIVER" != "gke" ] ; then
  sh "$SCDIR/load-image.sh" "busybox" "1"
  sh "$SCDIR/load-image.sh" "bitnami/kubectl" "1.23.6-debian-10-r0"
  case $DATABASE in
  "mariadb")
    sh "$SCDIR/load-image.sh" "mariadb" "10.4"
    ;;
  "postgres" | "postgresql")
    sh "$SCDIR/load-image.sh" "postgresql" "10"
    ;;
  *)
    echo "DATABASE=$DATABASE not supported"
    ;;
  esac
  case $BROKER in
  "kafka")
    sh "$SCDIR/load-image.sh" "confluentinc/cp-kafka" "5.5.2"
    sh "$SCDIR/load-image.sh" "confluentinc/cp-zookeeper" "5.5.2"
    ;;
  "rabbit" | "rabbitmq")
    sh "$SCDIR/load-image.sh" "rabbitmq" "3.6.10"
    ;;
  *)
    echo "BROKER=$BROKER not supported"
    ;;
  esac

  sh "$SCDIR/load-image.sh" "springcloud/spring-cloud-dataflow-composed-task-runner" "$DATAFLOW_VERSION" true
  sh "$SCDIR/load-image.sh" "springcloud/spring-cloud-skipper-server" "$SKIPPER_VERSION" true

  if [ "$USE_PRO" = "true" ]; then
    sh "$SCDIR/load-image.sh" "springcloud/scdf-pro-server" "$SCDF_PRO_VERSION" true
  else
    sh "$SCDIR/load-image.sh" "springcloud/spring-cloud-dataflow-server" "$DATAFLOW_VERSION" true
  fi
fi

pushd "$PARENT" > /dev/null

case $BROKER in
  "kafka")
    kubectl create --namespace "$NS" -f src/kubernetes/kafka/
    ;;
  "rabbit" | "rabbitmq")
    kubectl create --namespace "$NS" -f src/kubernetes/rabbitmq/
    ;;
  *)
    echo "BROKER=$BROKER not supported"
    ;;
esac
# TODO add support for PostgreSQL
kubectl create --namespace "$NS" -f src/kubernetes/mariadb/

if [ "$PROMETHEUS" = "true" ]; then
  echo "Loading Prometheus and Grafana"
  if [ "$K8S_DRIVER" != "tmc" ] && [ "$K8S_DRIVER" != "gke" ] ; then
    sh "$SCDIR/load-image.sh" "springcloud/spring-cloud-dataflow-grafana-prometheus" "2.10.1-SNAPSHOT" false
    sh "$SCDIR/load-image.sh" "prom/prometheus" "v2.12.0"
    sh "$SCDIR/load-image.sh" "micrometermetrics/prometheus-rsocket-proxy" "0.11.0"
  fi
  kubectl create --namespace "$NS" -f src/kubernetes/prometheus/prometheus-clusterroles.yaml
  kubectl create --namespace "$NS" -f src/kubernetes/prometheus/prometheus-clusterrolebinding.yaml
  kubectl create --namespace "$NS" -f src/kubernetes/prometheus/prometheus-serviceaccount.yaml
  kubectl create --namespace "$NS" -f src/kubernetes/prometheus-proxy/
  kubectl create --namespace "$NS" -f src/kubernetes/prometheus/prometheus-configmap.yaml
  kubectl create --namespace "$NS" -f src/kubernetes/prometheus/prometheus-deployment.yaml
  kubectl create --namespace "$NS" -f src/kubernetes/prometheus/prometheus-service.yaml
  kubectl create --namespace "$NS" -f src/kubernetes/grafana/
fi

# Deploy Spring Cloud Dataflow
set +e
kubectl create --namespace "$NS" -f src/kubernetes/server/server-roles.yaml
kubectl create --namespace "$NS" -f src/kubernetes/server/server-rolebinding.yaml
kubectl create --namespace "$NS" -f src/kubernetes/server/service-account.yaml
kubectl apply --namespace "$NS" -f "$K8S_PATH/server-config.yaml"
# Deploy Spring Cloud Skipper
if [ "$BROKER" = "kafka" ]; then
  kubectl apply --namespace "$NS" -f "$K8S_PATH/skipper-config-kafka.yaml"
else
  kubectl apply --namespace "$NS" -f "$K8S_PATH/skipper-config-rabbit.yaml"
fi
kubectl create --namespace "$NS" -f "$K8S_PATH/skipper-deployment.yaml"
kubectl create --namespace "$NS" -f "$K8S_PATH/skipper-svc.yaml"

# Start DataFlow
kubectl create --namespace "$NS" clusterrolebinding scdftestrole --clusterrole cluster-admin --user=system:serviceaccount:default:scdf-sa

kubectl create --namespace "$NS" -f "$K8S_PATH/server-svc.yaml"
if [ "$USE_PRO" = "true" ]; then
  kubectl create --namespace "$NS" -f "$K8S_PATH/server-deployment-pro.yaml"
else
  kubectl create --namespace "$NS" -f "$K8S_PATH/server-deployment.yaml"
fi

popd > /dev/null
