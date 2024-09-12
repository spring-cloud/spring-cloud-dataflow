#!/usr/bin/env bash
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "env var $1 not defined"
    exit 1
  fi
}

PACKAGE=$1
PACKAGE_NAME=$2
if [ "$3" != "" ]; then
    NS=$3
fi
check_env NS
check_env PACKAGE
check_env PACKAGE_NAME

echo "Adding $PACKAGE as $PACKAGE_NAME in $NS"

if [ "$DEBUG" = "true" ]; then
    ARGS="--debug"
else
    ARGS=""
fi
echo "Creating $PACKAGE_NAME for $PACKAGE"
if [ "$REPO_SECRET_REF" = "" ]; then
  if [[ "$PACKAGE_NAME" == *"pro"* ]]; then
    REPO_SECRET_REF=reg-creds-dev-registry
  else
    REPO_SECRET_REF=reg-creds-dockerhub
  fi
fi

echo "Using secretRef=$REPO_SECRET_REF in $PACKAGE_NAME for $PACKAGE"
set -e
kubectl apply --namespace $NS -f -  <<EOF
apiVersion: packaging.carvel.dev/v1alpha1
kind: PackageRepository
metadata:
  name: $PACKAGE_NAME
spec:
  fetch:
    imgpkgBundle:
      image: $PACKAGE
      secretRef:
        name: $REPO_SECRET_REF
EOF
kctrl package repository kick --namespace $NS --repository $PACKAGE_NAME --yes --wait --wait-check-interval 10s
kctrl package repository list --namespace $NS
