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

if [ "$1" != "" ]; then
    NS=$1
fi
check_env NS

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
$SCDIR/add-roles.sh "system:aggregate-to-edit" "system:aggregate-to-admin" "system:aggregate-to-view"
PRESENT=$(kubectl get serviceaccount --namespace $NS --output=json | count_kind serviceaccount "$NS-sa")
if ((PRESENT > 0)); then
  kubectl delete serviceaccount "$NS-sa" --namespace $NS
fi
kubectl create serviceaccount "$NS-sa" --namespace $NS
