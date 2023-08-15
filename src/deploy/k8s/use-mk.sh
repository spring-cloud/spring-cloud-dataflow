#!/usr/bin/env bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
if [ "$1" = "" ]; then
  echo "Arguments <driver> [--namespace <namespace>] [database] [broker]"
  echo "Driver must be one of kind, Or a valid driver for minikube like kvm2, docker, vmware, virtualbox, podman, vmwarefusion, hyperkit"
  return 0
fi
USE_PRO=false
K8S_DRIVER=$1
KUBECONFIG=
NS=scdf
METRICS=
SKIP_REG=
shift
while [ "$1" != "" ]; do
    case "$1" in
    "postgres" | "postgresql")
        DATABASE=postgresql
        ;;
    "maria" | "mariadb")
        DATABASE=mariadb
        ;;
    "rabbit" | "rabbitmq")
        BROKER=rabbitmq
        ;;
    "kafka")
        BROKER=kafka
        ;;
    "prometheus" | "grafana")
        METRICS=prometheus
        ;;
    "--skip-reg")
        export SKIP_REG=true
        ;;
    "--pro")
        export USE_PRO=true
        ;;
    "--namespace" | "-ns")
        if [ "$2" == "" ]; then
            echo "Expected <namespace> after $1"
            return 0
        fi
        export NS=$2
        shift
        ;;
    *)
        echo "Unknown option $2"
        return 0
    esac
    shift
done
echo "Namespace: $NS"
export NS
if [ "$BROKER" != "" ]; then
    echo "Broker: $BROKER"
    export BROKER
fi
if [ "$DATABASE" != "" ]; then
    echo "Database: $DATABASE"
    export DATABASE
fi
if [ "$METRICS" != "" ]; then
    echo "Metrics: $METRICS"
    export METRICS
fi
export K8S_DRIVER
export KUBECONFIG

