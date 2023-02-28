#!/usr/bin/env bash
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "env var $1 not defined"
    exit 1
  fi
}

SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

# the following names are your choice.
if [ "$NS" = "" ]; then
  echo "Expected env var NS"
  exit 1
fi
SA=$NS-sa

check_env INTERNAL_REGISTRY

REGISTRY=$INTERNAL_REGISTRY
PACKAGE_VERSION=1.5.2-SNAPSHOT
PACKAGE_NAME=scdfpro.tanzu.vmware.com
REPO_NAME="p-scdf-for-kubernetes/scdf-pro-repo"
APP_NAME=scdf-pro

PACKAGE="$REGISTRY/$REPO_NAME:$PACKAGE_VERSION"
set +e
kctrl package install --package-install $APP_NAME \
  --service-account-name $SA \
  --package $PACKAGE_NAME \
  --values-file "$SCDIR/scdf-pro-values.yml" \
  --version $PACKAGE_VERSION --namespace $NS --yes \
  --wait --wait-check-interval 10s

kctrl app status --app $APP_NAME --namespace $NS --json
kctrl package installed status --package-install $APP_NAME --namespace $NS
