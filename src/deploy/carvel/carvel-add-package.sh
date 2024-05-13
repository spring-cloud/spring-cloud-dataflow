#!/usr/bin/env bash
function check_env() {
  eval ev='$'$1
  if [ "$ev" = "" ]; then
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
kctrl package repository add --namespace $NS --repository $PACKAGE_NAME --url $PACKAGE --yes --wait --wait-check-interval 10s $ARGS
kctrl package repository list --namespace $NS
