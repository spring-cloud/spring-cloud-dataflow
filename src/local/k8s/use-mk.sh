#!/bin/bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
if [ "$1" = "" ]; then
  echo "Arguments <driver> [--namespace <namespace>] [database] [broker]"
  echo "Driver must be one of kind, Or a valid driver for minikube like kvm2, docker, vmware, virtualbox, podman, vmwarefusion, hyperkit"
  return 2
fi
export K8S_DRIVER=$1
export KUBECONFIG=
export NS=default
shift
while [ "$1" != "" ]; do
    case "$1" in
    "postgres" | "postgresql")
      export DATABASE=postgresql
      ;;
  "maria" | "mariadb")
        export DATABASE=mariadb
        ;;
    "rabbit" | "rabbitmq")
        export BROKER=rabbitmq
        ;;
    "kafka")
        export  BROKER=kafka
        ;;
    "--namespace" | "-ns")
        if [ "$2" == "" ]; then
            echo "Expected <namespace> after $1"
            exit 0
        fi
        export NS=$2
        shift
        ;;
    *)
        echo "Unknown option $2"
        exit 0
    esac
    shift
done
echo "Namespace: $NS"
if [ "$BROKER" != "" ]; then
    echo "Broker: $BROKER"
fi
if [ "$DATABASE" != "" ]; then
    echo "Database: $DATABASE"
fi
