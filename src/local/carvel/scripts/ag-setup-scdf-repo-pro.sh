#!/usr/bin/env bash
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
    --namespace secrets-ns
  kubectl create secret docker-registry "$1" \
        --docker-server="$2" \
        --docker-username="$3" \
        --docker-password="$4" \
        --namespace $NS
}

function count_kind() {
    jq --arg kind $1 --arg name $2 '.items | .[] | select(.kind == $kind) | .metadata | select(.name == $name) | .name' | grep -c -F "$2"
}

SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

# the following names are your choice.
check_env NS
SA=$NS-sa

PRESENT=$(kubectl get namespace --output=json | count_kind Namespace "$NS")
if ((PRESENT > 0)); then
  kubectl delete namespace $NS
fi

PRESENT=$(kubectl get namespace --output=json | count_kind Namespace "secret-ns")
if ((PRESENT > 0)); then
  kubectl delete namespace secrets-ns
fi

kubectl create namespace $NS
kubectl create namespace secrets-ns



# check_env DOCKER_HUB_USERNAME
# check_env DOCKER_HUB_PASSWORD

$SCDIR/add-roles.sh "system:aggregate-to-edit" "system:aggregate-to-admin" "system:aggregate-to-view"

PRESENT=$(kubectl get serviceaccount --namespace $NS --output=json | count_kind serviceaccount "$NS-sa")
if ((PRESENT > 0)); then
  kubectl delete serviceaccount "$NS-sa" --namespace $NS
fi
kubectl create serviceaccount "$NS-sa" --namespace $NS


check_env INTERNAL_REGISTRY
REGISTRY=$INTERNAL_REGISTRY
PACKAGE_VERSION=1.5.2-SNAPSHOT
PACKAGE_NAME=scdfpro.tanzu.vmware.com
REPO_NAME="p-scdf-for-kubernetes/scdf-pro-repo"

PACKAGE="$REGISTRY/$REPO_NAME:$PACKAGE_VERSION"

kubectl apply -f "$SCDIR/ag-secret-gen-export.yml" --namespace secrets-ns

check_env INTERNAL_REGISTRY_USERNAME
check_env INTERNAL_REGISTRY_PASSWORD

create_secret reg-creds-dev-registry $REGISTRY "$INTERNAL_REGISTRY_USERNAME" "$INTERNAL_REGISTRY_PASSWORD"
echo "Annotating reg-creds-dev-registry for image-pull-secret"
kubectl annotate secret reg-creds-dev-registry --namespace secrets-ns  secretgen.carvel.dev/image-pull-secret=""

echo "Adding $PACKAGE as $PACKAGE_NAME in $NS"
kctrl package repository add --namespace $NS --repository $PACKAGE_NAME --url $PACKAGE --yes --wait --wait-check-interval 10s
kctrl package repository list --namespace $NS
