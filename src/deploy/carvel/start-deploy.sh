#!/usr/bin/env bash
bold="\033[1m"
dim="\033[2m"
end="\033[0m"
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
    echo "This script must be invoked using: source $0 $*"
    exit 1
fi
if [ "$1" = "" ]; then
    echo "Usage: <broker> [scdf-type] [namespace] [release|snapshot]"
    echo "Where:"
    echo "  broker is one of kafka or rabbitmq"
    echo "  scdf-type is one of oss or pro. The default is 'oss'"
    echo "  namespace is a valid k8s namespace other than 'default'. The default is 'scdf'."
    echo "  release or snapshot and scdf-type will determine PACKAGE_VERSION. The default is latest snapshot."
    return 0
fi

SCDF_TYPE=oss
NS=scdf

while [ "$1" != "" ]; do
    case $1 in
    "snapshot")
        if [ "$SCDF_TYPE" = "oss" ]; then
            PACKAGE_VERSION=2.11.0-SNAPSHOT
        else
            PACKAGE_VERSION=1.6.0-SNAPSHOT
        fi
        export PACKAGE_VERSION
        ;;
    "release")
        if [ "$SCDF_TYPE" = "oss" ]; then
            PACKAGE_VERSION=2.10.3
        else
            PACKAGE_VERSION=1.5.3
        fi
        export PACKAGE_VERSION
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

export BROKER
export SCDF_TYPE
export NS

echo "Broker: $BROKER"
echo "SCDF Type: $SCDF_TYPE"
if [ "$PACKAGE_VERSION" = "" ]; then
    echo "Package version: N/A"
    echo -e "${bold}Configure environmental variable PACKAGE_VERSION to specific value.${end}"
else
    echo "Package version: $PACKAGE_VERSION"
fi
echo "NS: $NS"
