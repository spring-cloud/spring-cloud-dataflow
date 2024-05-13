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

bold="\033[1m"
dim="\033[2m"
end="\033[0m"
(return 0 2>/dev/null) && sourced=1 || sourced=0
if [ "$sourced" = "0" ]; then
    echo "This script must be invoked using: source $0 $*"
    exit 1
fi

SCDF_TYPE=$(yq '.default.scdf-type' $SCDIR/../versions.yaml)
SCDF_REL=$(yq '.default.version' $SCDIR/../versions.yaml)
NS=scdf
DEFAULT_PACKAGE_VERSION=$(yq ".default.package-version" "$SCDIR/../versions.yaml")
if [ "$DEFAULT_PACKAGE_VERSION" = "" ] || [ "$DEFAULT_PACKAGE_VERSION" = "null" ]; then
    DEFAULT_PACKAGE_VERSION=$(yq ".scdf-type.$SCDF_TYPE.$SCDF_REL" "$SCDIR/../versions.yaml")
fi
if [ "$1" = "" ]; then
    echo "Usage: <broker> [scdf-type] [namespace] [release|snapshot|maintenance-snapshot]"
    echo "Where:"
    echo "  broker is one of kafka or rabbitmq"
    echo "  scdf-type is one of oss or pro. The default is '$SCDF_TYPE'"
    echo "  namespace is a valid k8s namespace other than 'default'. The default is '$NS'."
    echo "  release, snapshot or maintenance-snapshot and scdf-type will determine PACKAGE_VERSION. The default is $DEFAULT_PACKAGE_VERSION."
    return 0
fi

while [ "$1" != "" ]; do
    case $1 in
    "snapshot" | "release" | "maintenance-snapshot")
        SCDF_REL=$1
        export PACKAGE_VERSION=
        ;;
    "rabbitmq" | "rabbit")
        BROKER=rabbitmq
        ;;
    "kafka")
        BROKER=kafka
        ;;
    "pro" | "oss")
        SCDF_TYPE=$1
        export PACKAGE_VERSION=
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
if [ "$DEBUG" = "true" ]; then
    echo "yq '.scdf-type.$SCDF_TYPE.$SCDF_REL' $SCDIR/../versions.yaml"
fi
if [ "$PACKAGE_VERSION" = "" ]; then
    PACKAGE_VERSION=$(yq ".default.package-version" "$SCDIR/../versions.yaml")
fi
if [ "$PACKAGE_VERSION" = "null" ] || [ "$PACKAGE_VERSION" = "" ]; then
    PACKAGE_VERSION="$(yq ".scdf-type.$SCDF_TYPE.$SCDF_REL" "$SCDIR/../versions.yaml")"
fi
export PACKAGE_VERSION
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
