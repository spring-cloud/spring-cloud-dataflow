#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
start_time=$(date +%s)
# the following names are your choice.
if [ "$NS" = "" ]; then
  echo "Expected env var NS"
  exit 1
fi
COUNT=$(kubectl get namespace $NS | grep -c "$NS")
if ((COUNT == 0)); then
    echo "Expected namespace $NS"
    exit 2
else
    echo "Namespace $NS exists"
fi

SA=$NS-sa
if [ "$SCDF_TYPE" == "" ]; then
    echo "SCDF_TYPE must be set to one of oss or pro."
fi

case $SCDF_TYPE in
"pro")
  APP_NAME=scdf-pro
  PACKAGE_VERSION=1.5.3-SNAPSHOT
  PACKAGE_NAME=scdfpro.tanzu.vmware.com
  ;;
"oss")
  APP_NAME=scdf-oss
  PACKAGE_VERSION=2.11.0-SNAPSHOT
  PACKAGE_NAME=scdf.tanzu.vmware.com
  ;;
*)
  echo "Invalid SCDF_TYPE=$SCDF_TYPE only pro or oss is acceptable"
esac

echo "Deploying scdf-$SCDF_TYPE $PACKAGE_NAME:$PACKAGE_VERSION as $APP_NAME"
set +e
$SCDIR/carvel-deploy-package.sh $APP_NAME $PACKAGE_NAME $PACKAGE_VERSION "$SCDIR/scdf-values.yml" "$NS"
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Deployed SCDF in ${bold}$elapsed${end} seconds"
