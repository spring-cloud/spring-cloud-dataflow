#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "env var $1 not defined"
    exit 1
  fi
}


if [ "$4" = "" ]; then
    echo "Arguments: <secret-name> <registry-name> <registry-user> <registry-password>"
    exit 1
fi
SECRET_NAME=$1
REGISTRY_NAME=$2
REGISTRY_USER=$3
REGISTRY_PWD=$4
if [ "$5" != "" ]; then
    NS=$5
fi
check_env NS
kubectl create secret docker-registry "$SECRET_NAME" \
    --docker-server="$REGISTRY_NAME" \
    --docker-username="$REGISTRY_USER" \
    --docker-password="$REGISTRY_PWD" \
    --namespace "$NS"


