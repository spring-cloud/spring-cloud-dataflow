#!/usr/bin/env bash
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
    SA=$NS-sa
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

kctrl package install --package-install "$APP_NAME" \
  --service-account-name "$SA" \
  --package "$PACKAGE_NAME" \
  --values-file "$VALUES_FILE" \
  --version "$PACKAGE_VERSION" --namespace "$NS" --yes \
  --wait --wait-check-interval 10s

kctrl app status --app "$APP_NAME" --namespace "$NS" --json
kctrl package installed status --package-install "$APP_NAME" --namespace "$NS"
