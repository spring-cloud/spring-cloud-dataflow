#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

function check_env() {
    eval ev='$'$1
    if [ "$ev" == "" ]; then
        echo "env var $1 not defined"
        exit 1
    fi
}
function count_kind() {
    jq --arg kind $1 --arg name $2 '.items | .[] | select(.kind == $kind) | .metadata | select(.name == $name) | .name' | grep -c -F "$2"
}

function patch_serviceaccount() {
  if [ "$2" != "" ]; then
    kubectl patch serviceaccount "$2" -p "$1" --namespace "$NS"
  fi
  kubectl patch serviceaccount default -p "$1" --namespace "$NS"
}
if [ "$1" = "" ]; then
    echo "Usage: <namespace> [service-account-name]"
    exit 1
fi
NS=$1
if [ "$2" != "" ]; then
    SA=$2
fi
readonly DOCKER_HUB_USERNAME="${DOCKER_HUB_USERNAME:?must be set}"
readonly DOCKER_HUB_PASSWORD="${DOCKER_HUB_PASSWORD:?must be set}"
if [ "$SCDF_TYPE" = "pro" ]; then
  readonly TANZU_DOCKER_USERNAME="${TANZU_DOCKER_USERNAME:?must be set}"
  readonly TANZU_DOCKER_PASSWORD="${TANZU_DOCKER_PASSWORD:?must be set}"
fi

kubectl create namespace "$NS"

$SCDIR/add-roles.sh "system:aggregate-to-edit" "system:aggregate-to-admin" "system:aggregate-to-view"
if [ "$SA" != "" ]; then
  kubectl create serviceaccount "$SA" --namespace "$NS"
fi

$SCDIR/add-local-registry-secret.sh scdfmetadata index.docker.io "$DOCKER_HUB_USERNAME" "$DOCKER_HUB_PASSWORD"
$SCDIR/add-local-registry-secret.sh reg-creds-dockerhub index.docker.io "$DOCKER_HUB_USERNAME" "$DOCKER_HUB_PASSWORD"
patch_serviceaccount '{"imagePullSecrets": [{"name": "reg-creds-dockerhub"},{"name":"scdfmetadata"}]}' "$SA"

if [ "$SCDF_TYPE" = "pro" ]; then
  $SCDIR/carvel-add-registry-secret.sh reg-creds-dev-registry registry.packages.broadcom.com "$TANZU_DOCKER_USERNAME" "$TANZU_DOCKER_PASSWORD"
  $SCDIR/carvel-add-registry-secret.sh reg-creds-dev-registry spring-scdf-docker-virtual.usw1.packages.broadcom.com "$TANZU_DOCKER_USERNAME" "$TANZU_DOCKER_PASSWORD"
  patch_serviceaccount '{"imagePullSecrets": [{"name": "reg-creds-dev-registry"}]}' "$SA"
fi
