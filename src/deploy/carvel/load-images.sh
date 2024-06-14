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

PROMETHEUS=$(yq ".scdf.feature.monitoring.prometheusRsocketProxy.enabled" ./scdf-values.yml)
if [ "$PROMETHEUS" = "null" ]; then
    PROMETHEUS=false
fi
SCDF_TYPE=oss
while [ "$1" != "" ]; do
    case $1 in
    "prometheus")
        PROMETHEUS=true
        ;;
    "mariadb" | "maria")
        DATABASE=mariadb
        ;;
    "postgres" | "postgresql")
        DATABASE=postgresql
        ;;
    "rabbit" | "rabbitmq")
        BROKER=rabbitmq
        ;;
    "kafka")
        BROKER=kafka
        ;;
    "oss" | "pro")
        SCDF_TYPE=$1
    esac
    shift
done

K8S=$(realpath $SCDIR/../k8s)
case $DATABASE in
"mariadb")
    sh "$K8S/load-image.sh" "mariadb" "10.6" false
    ;;
"postgresql")
    sh "$K8S/load-image.sh" "postgres" "12" false
    ;;
*)
    echo "DATABASE=$DATABASE not supported"
    ;;
esac
case $BROKER in
"kafka")
    sh "$K8S/load-image.sh" "confluentinc/cp-kafka" "5.5.2" false
    sh "$K8S/load-image.sh" "confluentinc/cp-zookeeper" "5.5.2" false
    ;;
"rabbit" | "rabbitmq")
    sh "$K8S/load-image.sh" "rabbitmq" "3.8-management" false
    ;;
*)
    echo "BROKER=$BROKER not supported"
    ;;
esac
sh "$K8S/load-image.sh" "springcloud/spring-cloud-dataflow-composed-task-runner" "$DATAFLOW_VERSION" true
sh "$K8S/load-image.sh" "springcloud/spring-cloud-skipper-server" "$DATAFLOW_VERSION" true

if [ "$SCDF_TYPE" = "pro" ]; then
    if [ "$SCDF_PRO_VERSION" != "" ]; then
        sh "$K8S/load-image.sh" "dev.registry.tanzu.vmware.com/p-scdf-for-kubernetes/scdf-pro-server" "$SCDF_PRO_VERSION" true
    fi
else
    sh "$K8S/load-image.sh" "springcloud/spring-cloud-dataflow-server" "$DATAFLOW_VERSION" true
fi
if [ "$PROMETHEUS" = "true" ]; then
    sh "$K8S/load-image.sh" "micrometermetrics/prometheus-rsocket-proxy" "1.5.2" false
fi
if [ "$REGISTRY" = "" ]; then
    REGISTRY=springcloud
fi

sh "$K8S/load-image.sh" "$REGISTRY/scdf-${SCDF_TYPE}-repo" "$DATAFLOW_VERSION" true
# Task Apps
echo "Loading Task Apps images"
sh "$K8S/load-image.sh" "springcloudtask/timestamp-task" "2.0.2" true
sh "$K8S/load-image.sh" "springcloudtask/timestamp-task" "3.0.0" true
sh "$K8S/load-image.sh" "springcloudtask/timestamp-batch-task" "2.0.2" true
sh "$K8S/load-image.sh" "springcloudtask/timestamp-batch-task" "3.0.0" true
