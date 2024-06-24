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
    kubectl patch serviceaccount $SA -p "$1" --namespace "$NS"
    kubectl patch serviceaccount default -p "$1" --namespace "$NS"
}
if [ "$1" = "" ]; then
    echo "Usage: <service-account-name> [namespace]"
    exit 1
fi
if [ "$2" != "" ]; then
    NS=$2
fi

check_env NS
SA=$1
kubectl create namespace $NS

$SCDIR/add-roles.sh "system:aggregate-to-edit" "system:aggregate-to-admin" "system:aggregate-to-view"

kubectl create serviceaccount "$SA" --namespace $NS

$SCDIR/add-local-registry-secret.sh scdfmetadata docker.io "$DOCKER_HUB_USERNAME" "$DOCKER_HUB_PASSWORD"
$SCDIR/add-local-registry-secret.sh reg-creds-dockerhub docker.io "$DOCKER_HUB_USERNAME" "$DOCKER_HUB_PASSWORD"
patch_serviceaccount '{"imagePullSecrets": [{"name": "reg-creds-dockerhub"},{"name":"scdfmetadata"}]}'

if [ "$SCDF_TYPE" = "pro" ]; then
    if [ "$TANZU_DOCKER_USERNAME" = "" ]; then
        echo "Cannot find TANZU_DOCKER_USERNAME"
    else
        check_env TANZU_DOCKER_PASSWORD
        $SCDIR/carvel-add-registry-secret.sh reg-creds-dev-registry registry.packages.broadcom.com "$TANZU_DOCKER_USERNAME" "$TANZU_DOCKER_PASSWORD"
        $SCDIR/carvel-add-registry-secret.sh reg-creds-dev-registry spring-scdf-docker-virtual.usw1.packages.broadcom.com "$TANZU_DOCKER_USERNAME" "$TANZU_DOCKER_PASSWORD"
        patch_serviceaccount '{"imagePullSecrets": [{"name": "reg-creds-dev-registry"}]}'
    fi
fi
