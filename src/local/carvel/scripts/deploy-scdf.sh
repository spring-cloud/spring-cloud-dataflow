#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")

# the following names are your choice.
if [ "$NS" = "" ]; then
  echo "Expected env var NS"
  exit 1
fi
SA=$NS-sa
if [ "$SCDF_TYPE" == "" ]; then
    SCDF_TYPE=pro
fi
if [ "$1" != "" ]; then
  SCDF_TYPE=$1
fi

case $SCDF_TYPE in
"pro")
  APP_NAME=scdf-pro
  PACKAGE_VERSION=1.5.3-SNAPSHOT
  PACKAGE_NAME=scdfpro.tanzu.vmware.com
  REGISTRY=dev.registry.pivotal.io
  REPO_NAME="p-scdf-for-kubernetes/scdf-pro-repo"
  ;;
"oss")
  APP_NAME=scdf-oss
  PACKAGE_VERSION=2.10.3-SNAPSHOT
  PACKAGE_NAME=scdf.tanzu.vmware.com
  REGISTRY=index.docker.io
  REPO_NAME="springcloud/scdf-repo"
  ;;
*)
  echo "Invalid SCDF_TYPE=$SCDF_TYPE only pro or oss is acceptable"
esac

echo "Deploying SCDF-$SCDF_TYPE $PACKAGE_NAME:$PACKAGE_VERSION as $APP_NAME"
set +e
kctrl package install --package-install $APP_NAME \
  --service-account-name $SA \
  --package $PACKAGE_NAME \
  --values-file "$SCDIR/scdf-${SCDF_TYPE}-values.yml" \
  --version $PACKAGE_VERSION --namespace $NS --yes \
  --wait --wait-check-interval 10s

kctrl app status --app $APP_NAME --namespace $NS --json
kctrl package installed status --package-install $APP_NAME --namespace $NS
