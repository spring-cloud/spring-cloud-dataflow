#!/bin/bash
if [ "$NS" = "" ]; then
    echo "NS not defined" >&2
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
K8S=$(realpath $SCDIR/../kubernetes)
if [ ! -d "$K8S" ]; then
  K8S=$(realpath $SCDIR/../../kubernetes)
fi
PARENT=$(realpath "$SCDIR/../../..")
if [ "$DATAFLOW_PRO_VERSION" = "" ]; then
  DATAFLOW_PRO_VERSION=1.6.0-SNAPSHOT
fi
if [ "$DATAFLOW_VERSION" = "" ]; then
  export DATAFLOW_VERSION=2.11.0-SNAPSHOT
fi
if [ "$SKIPPER_VERSION" = "" ]; then
  export SKIPPER_VERSION=2.11.0-SNAPSHOT
fi

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
    # default is MariaDB
    ;;

"postgres" | "postgresql")
    export DATABASE=postgresql
    ;;

"")
    # default is MariaDB
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
if [ "$K8S_DRIVER" != "tmc" ] && [ "$K8S_DRIVER" != "gke" ]; then
    if [ "$DOCKER_USER" = "" ] || [ "$DOCKER_SERVER" = "" ] || [ "$DOCKER_PASSWORD" = "" ]; then
        echo "DOCKER_SERVER, DOCKER_USER, DOCKER_PASSWORD, DOCKER_EMAIL is required" >&2
        exit 1
    fi
    kubectl create secret docker-registry registry-key --namespace "$NS" --docker-server=$DOCKER_SERVER --docker-username=$DOCKER_USER --docker-password=$DOCKER_PASSWORD --docker-email=$DOCKER_EMAIL
    kubectl patch serviceaccount default -p '{"imagePullSecrets": [{"name": "registry-key"}]}' --namespace "$NS"
fi

if [ "$USE_PRO" = "" ]; then
    USE_PRO=false
fi

if [ "$DATAFLOW_VERSION" = "" ]; then
    DATAFLOW_VERSION=2.11.0-SNAPSHOT
fi

if [ "$SKIPPER_VERSION" = "" ]; then
    SKIPPER_VERSION=2.11.0-SNAPSHOT
fi

if [ "$SCDF_PRO_VERSION" = "" ]; then
    SCDF_PRO_VERSION=1.6.0-SNAPSHOT
fi
YAML_PATH=$(realpath $SCDIR/yaml)

echo "YAML_PATH=$YAML_PATH"

set -e
if [ "$K8S_DRIVER" != "tmc" ] && [ "$K8S_DRIVER" != "gke" ]; then
    sh "$SCDIR/load-image.sh" "busybox" "1"
    # sh "$SCDIR/load-image.sh" "bitnami/kubectl" "1.23.6-debian-10-r0"
    case $DATABASE in
    "mariadb")
        sh "$SCDIR/load-image.sh" "mariadb" "10"
        ;;
    "postgresql")
        sh "$SCDIR/load-image.sh" "postgres" "12"
        ;;
    *)
        echo "DATABASE=$DATABASE not supported"
        ;;
    esac
    case $BROKER in
    "kafka")
        sh "$SCDIR/load-image.sh" "confluentinc/cp-kafka" "5"
        sh "$SCDIR/load-image.sh" "confluentinc/cp-zookeeper" "5"
        ;;
    "rabbit" | "rabbitmq")
        sh "$SCDIR/load-image.sh" "rabbitmq" "3.8"
        ;;
    *)
        echo "BROKER=$BROKER not supported"
        ;;
    esac

    sh "$SCDIR/load-image.sh" "springcloud/spring-cloud-dataflow-composed-task-runner" "$DATAFLOW_VERSION" true
    sh "$SCDIR/load-image.sh" "springcloud/spring-cloud-skipper-server" "$SKIPPER_VERSION" true

    if [ "$USE_PRO" = "true" ]; then
        sh "$SCDIR/load-image.sh" "dev.registry.pivotal.io/p-scdf-for-kubernetes/scdf-pro-server" "$SCDF_PRO_VERSION" true
    else
        sh "$SCDIR/load-image.sh" "springcloud/spring-cloud-dataflow-server" "$DATAFLOW_VERSION" true
    fi
fi

pushd "$PARENT" >/dev/null

case $BROKER in
"kafka")

    kubectl create --namespace "$NS" -f $K8S/kafka/
    ;;
"rabbit" | "rabbitmq")
    kubectl create --namespace "$NS" -f $K8S/rabbitmq/
    ;;
*)
    echo "BROKER=$BROKER not supported"
    ;;
esac
kubectl create --namespace "$NS" -f $K8S/$DATABASE/


if [ "$PROMETHEUS" = "true" ] || [ "$METRICS" = "prometheus" ]; then
    echo "Loading Prometheus and Grafana"
    if [ "$K8S_DRIVER" != "tmc" ] && [ "$K8S_DRIVER" != "gke" ]; then
        sh "$SCDIR/load-image.sh" "springcloud/spring-cloud-dataflow-grafana-prometheus" "2.11.0-SNAPSHOT" false
        sh "$SCDIR/load-image.sh" "prom/prometheus" "v2.37.8"
        sh "$SCDIR/load-image.sh" "micrometermetrics/prometheus-rsocket-proxy" "1.5.2"
    fi
    set +e
    kubectl create --namespace "$NS" serviceaccount prometheus-rsocket-proxy
    kubectl create --namespace "$NS" serviceaccount prometheus
    kubectl create --namespace "$NS" clusterrolebinding prometheus --clusterrole prometheus --user=prometheus
    kubectl create --namespace "$NS" clusterrolebinding prometheus-rsocket-proxy --clusterrole cluster-admin --user=prometheus-rsocket-proxy
    kubectl create --namespace "$NS" -f $K8S/prometheus/
    kubectl create --namespace "$NS" -f $K8S/prometheus-proxy/
    kubectl create --namespace "$NS" -f $K8S/grafana/
fi

# Deploy Spring Cloud Dataflow
set +e
kubectl create --namespace "$NS" -f $K8S/server/server-roles.yaml
kubectl create --namespace "$NS" -f $K8S/server/server-rolebinding.yaml
kubectl create --namespace "$NS" -f $K8S/server/service-account.yaml

kubectl create --namespace "$NS" -f "$YAML_PATH/datasource-config-$DATABASE.yaml"

kubectl apply --namespace "$NS" -f "$YAML_PATH/server-config.yaml"

# Deploy Spring Cloud Skipper
echo "Deploying Skipper $SKIPPER_VERSION for $BROKER and $DATABASE"
kubectl apply --namespace "$NS" -f "$YAML_PATH/skipper-config-$BROKER.yaml"
cat "$YAML_PATH/skipper-deployment.yaml" | envsubst '$SKIPPER_VERSION,$DATABASE' | kubectl create --namespace "$NS" -f -

kubectl create --namespace "$NS" -f "$YAML_PATH/skipper-svc.yaml"


kubectl create --namespace "$NS" clusterrolebinding scdftestrole --clusterrole cluster-admin --user=system:serviceaccount:default:scdf-sa

if [ "$USE_PRO" = "true" ]; then
    echo "Deploying Data Flow Server Pro $DATAFLOW_PRO_VERSION for $BROKER and $DATABASE"
    cat "$YAML_PATH/server-deployment-pro.yaml" | envsubst '$DATAFLOW_PRO_VERSION,$DATABASE' | kubectl create --namespace "$NS" -f -
else
    echo "Deploying Data Flow Server $DATAFLOW_VERSION for $BROKER and $DATABASE"
    cat "$YAML_PATH/server-deployment.yaml" | envsubst '$DATAFLOW_VERSION,$DATABASE' | kubectl create --namespace "$NS" -f -
fi


kubectl create --namespace "$NS" -f "$YAML_PATH/server-svc.yaml"

popd >/dev/null
