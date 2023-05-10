#!/usr/bin/env bash
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
    echo "This script must be invoked using: source $0 $*"
    exit 1
fi
if [ "$1" = "" ]; then
    echo "Usage: [database] <broker> [scdf-type] <namespace>"
    echo "Where:"
    echo "  database is one of postgresql or mariadb"
    echo "  broker is one of kafka or rabbitmq"
    echo "  scdf-type is one of oss or pro"
    echo "  namespace is a valid k8s namespace other than default"
    return 0
fi
DATABASE=postgresql
SCDF_TYPE=oss
while [ "$1" != "" ]; do
    case $1 in
    "postgresql" | "postgres")
        DATABASE=postgresql
        ;;
    "mariadb")
        DATABASE=mariadb
        ;;
    "rabbitmq" | "rabbit")
        BROKER=rabbitmq
        ;;
    "kafka")
        BROKER=kafka
        ;;
    "pro" | "oss")
        SCDF_TYPE=$1
        ;;
    *)
        NS=$1
        ;;
    esac
    shift
done
if [ "$NS" = "" ]; then
    echo "Namespace must be provided"
    return 0
fi
if [ "$BROKER" = "" ]; then
    echo "Broker must be provided"
    return 0
fi
if [ "$DATABASE" != "" ]; then
    echo "Database: $DATABASE"
    export DATABASE
fi
export BROKER
export SCDF_TYPE
export NS
echo "Broker: $BROKER"
echo "SCDF Type: $SCDF_TYPE"
echo "NS: $NS"
