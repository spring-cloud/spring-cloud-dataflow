#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
  echo "This script must be invoked using: source $0 $*"
  exit 1
fi
if [ "$1" = "" ]; then
  echo "Usage $0 [--release | --snapshot | --milestone] [--pro] [--skip-reg] [--namespace <namespace> | -ns <namespace>] [postgres | postgresql | maria | mariadb | mysql | mysql57] [rabbit | rabbitmq | kafka] [prometheus | grafana]"
  return 0
fi
METRICS=
SKIP_REG=
VERSION_FILE=$(realpath "$SCDIR/../versions.yaml")
VERSION_TYPE=$(cat "$VERSION_FILE" | yq '.default.version')
SCDF_TYPE=$(cat "$VERSION_FILE" | yq '.default.scdf-type')
FORCE_VERSION=false
export USE_PRO=false
while [ "$1" != "" ]; do
    case "$1" in
    "postgres" | "postgresql")
        DATABASE=postgresql
        ;;
    "maria" | "mariadb")
        DATABASE=mariadb
        ;;
    "mysql" | "mysql57")
        DATABASE=mysql57
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
    "--release")
        VERSION_TYPE=release
        FORCE_VERSION=true
        ;;
    "--snapshot")
        VERSION_TYPE=snapshot
        FORCE_VERSION=true
        ;;
    "--milestone")
        VERSION_TYPE=milestone
        FORCE_VERSION=true
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
        echo "Unknown option $1"
        return 0
    esac
    shift
done
if [ "$NS" = "" ]; then
    NS=scdf
fi
echo "Namespace: $NS"
export NS
if [ "$BROKER" != "" ]; then
    echo "BROKER: $BROKER"
    export BROKER
fi
if [ "$DATABASE" != "" ]; then
    echo "DATABASE: $DATABASE"
    export DATABASE
fi
if [ "$METRICS" != "" ]; then
    echo "METRICS: $METRICS"
    export METRICS
fi
export K8S_DRIVER
export KUBECONFIG

if [ "$DATAFLOW_VERSION" = "" ] || [ "$FORCE_VERSION" = "true" ]; then
    if [ "$SCDF_TYPE" = "pro" ]; then
        DATAFLOW_PRO_VERSION=$(cat $VERSION_FILE | yq ".scdf-type.pro.${VERSION_TYPE}")
        export DATAFLOW_PRO_VERSION
        DATAFLOW_VERSION=$(cat $VERSION_FILE | yq ".scdf-type.oss.${VERSION_TYPE}")
    else
        DATAFLOW_VERSION=$(cat $VERSION_FILE | yq ".scdf-type.oss.${VERSION_TYPE}")
    fi
    SKIPPER_VERSION=$DATAFLOW_VERSION
    export DATAFLOW_VERSION
    export SKIPPER_VERSION
    echo "DATAFLOW_VERSION: $DATAFLOW_VERSION"
    echo "SKIPPER_VERSION: $SKIPPER_VERSION"
fi
