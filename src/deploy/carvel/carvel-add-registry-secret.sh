#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "env var $1 not defined"
    exit 1
  fi
}

function create_secret() {
    echo "Create docker-registry secret $1 for $2 username=$3"
    kubectl create secret docker-registry "$1" \
        --docker-server="$2" \
        --docker-username="$3" \
        --docker-password="$4" \
        --namespace "$5"
    kubectl create secret docker-registry "$1" \
            --docker-server="$2" \
            --docker-username="$3" \
            --docker-password="$4" \
            --namespace "$NS"
#    "$SCDIR/carvel-import-secret.sh" "$1" "$NS" "$5"
    echo "Annotating $1 for image-pull-secret"
    kubectl annotate secret "$1" --namespace "$5"  secretgen.carvel.dev/image-pull-secret=""
    kubectl annotate secret "$1" --namespace "$NS"  secretgen.carvel.dev/image-pull-secret=""

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
create_secret "$SECRET_NAME" "$REGISTRY_NAME" "$REGISTRY_USER" "$REGISTRY_PWD" "secrets-ns"
kubectl patch serviceaccount default -p '{"imagePullSecrets": [{"name": "registry-key"}]}' --namespace "$NS"
