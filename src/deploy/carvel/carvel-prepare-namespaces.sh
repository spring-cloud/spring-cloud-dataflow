#!/usr/bin/env bash
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
    kubectl patch serviceaccount scdf-sa -p "$1" --namespace "$NS"
    kubectl patch serviceaccount default -p "$1" --namespace "$NS"
    kubectl patch serviceaccount default -p "$1" --namespace default
}
if [ "$1" != "" ]; then
    NS=$1
fi
if [ "$2" != "" ]; then
    SA=$2
else
    SA=scdf-sa
fi
check_env NS

kubectl create namespace $NS
kubectl create namespace secrets-ns
$SCDIR/add-roles.sh "system:aggregate-to-edit" "system:aggregate-to-admin" "system:aggregate-to-view"
PRESENT=$(kubectl get serviceaccount --namespace $NS --output=json | count_kind serviceaccount "$NS-sa")
if ((PRESENT > 0)); then
    kubectl delete serviceaccount "$SA" --namespace $NS
fi
kubectl create serviceaccount "$SA" --namespace $NS

$SCDIR/carvel-add-registry-secret.sh scdf-metadata-default docker.io "$DOCKER_HUB_USERNAME" "$DOCKER_HUB_PASSWORD"
$SCDIR/carvel-add-registry-secret.sh reg-creds-dockerhub docker.io "$DOCKER_HUB_USERNAME" "$DOCKER_HUB_PASSWORD"


if [ "$SCDF_TYPE" = "pro" ]; then
    check_env TANZU_DOCKER_USERNAME
    check_env TANZU_DOCKER_PASSWORD
    $SCDIR/carvel-add-registry-secret.sh reg-creds-dev-registry dev.registry.pivotal.io "$TANZU_DOCKER_USERNAME" "$TANZU_DOCKER_PASSWORD"
    patch_serviceaccount '{"imagePullSecrets": [{"name": "reg-creds-dockerhub"},{"name": "reg-creds-dev-registry"}]}'
else
    patch_serviceaccount '{"imagePullSecrets": [{"name": "reg-creds-dockerhub"}]}'
fi
