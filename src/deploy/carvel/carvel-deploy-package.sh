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

APP_NAME=$1
PACKAGE_NAME=$2
PACKAGE_VERSION=$3
VALUES_FILE=$4
if [ "$4" = "" ]; then
    echo "Arguments: <app-name> <package-name> <package-version> <values-file> <namespace> <sa-account-name>"
    exit 1
fi
if [ "$5" != "" ]; then
    NS=$5
fi
if [ "$6" != "" ]; then
    SA=$6
else
    SA=scdf-sa
fi
if [ ! -f "$VALUES_FILE" ]; then
    echo "Cannot find $VALUES_FILE"
    exit 2
fi
check_env NS
check_env APP_NAME
check_env PACKAGE_NAME
check_env PACKAGE_VERSION
check_env VALUES_FILE
echo "Install package $PACKAGE_NAME as $APP_NAME"
if [ "$DEBUG" = "true" ]; then
    ARGS="--debug"
else
    ARGS=""
fi
SA_ARGS=
if [ "$SA" != "" ]; then
    SA_ARGS="--service-account-name $SA"
fi
echo "Installing $APP_NAME from $PACKAGE_NAME:$PACKAGE_VERSION"
# $SA_ARGS
kctrl package install --package-install "$APP_NAME" \
  --package "$PACKAGE_NAME" \
  --values-file "$VALUES_FILE" \
  --version "$PACKAGE_VERSION" --namespace "$NS" --yes \
  --wait --wait-check-interval 10s $ARGS
RC=$?
if ((RC!=0)); then
    kubectl --namespace "$NS" describe package/$PACKAGE_NAME
fi
kctrl app status --app "$APP_NAME" --namespace "$NS" --json
kctrl package installed status --package-install "$APP_NAME" --namespace "$NS"
