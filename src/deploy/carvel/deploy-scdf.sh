#!/usr/bin/env bash
bold="\033[1m"
dim="\033[2m"
end="\033[0m"
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "env var $1 not defined"
    if ((sourced != 0)); then
      return 1
    else
      exit 1
    fi
  fi
}
if [ "$SCDF_TYPE" == "" ]; then
    echo "Environmental variable SCDF_TYPE must be set to one of oss or pro."
fi
check_env NS
check_env PACKAGE_VERSION
check_env SCDF_TYPE
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
start_time=$(date +%s)
# the following names are your choice.

COUNT=$(kubectl get namespace $NS | grep -c "$NS")
if ((COUNT == 0)); then
    echo "Expected namespace $NS"
    exit 2
else
    echo "Namespace $NS exists"
fi

case $SCDF_TYPE in
"pro")
    APP_NAME=scdf-pro-app
    PACKAGE_NAME=scdf-pro.tanzu.vmware.com
    ;;
"oss")
    APP_NAME=scdf-oss-app
    PACKAGE_NAME=scdf.tanzu.vmware.com
    ;;
*)
    echo "Invalid SCDF_TYPE=$SCDF_TYPE only pro or oss is acceptable"
    ;;
esac
if [ "$1" != "" ]; then
    APP_NAME="$1"
fi
echo "Deploying scdf-$SCDF_TYPE $PACKAGE_NAME:$PACKAGE_VERSION as $APP_NAME"
if [ "$DATAFLOW_VERSION" != "" ]; then
    yq ".scdf.server.image.tag=\"$DATAFLOW_VERSION\"" -i ./scdf-values.yml
    yq ".scdf.ctr.image.tag=\"$DATAFLOW_VERSION\"" -i ./scdf-values.yml
    echo "Overriding Data Flow version=$DATAFLOW_VERSION"
fi
if [ "$SKIPPER_VERSION" != "" ]; then
    yq ".scdf.skipper.image.tag=\"$SKIPPER_VERSION\"" -i ./scdf-values.yml
    echo "Overriding Skipper version=$SKIPPER_VERSION"
fi

"$SCDIR/carvel-import-secret.sh" "scdfmetadata" "$NS"
"$SCDIR/carvel-import-secret.sh" "reg-creds-dockerhub" "$NS"

if [ "$SCDF_TYPE" = "pro" ]; then
    "$SCDIR/carvel-import-secret.sh" "reg-creds-dev-registry" "$NS"
fi
set +e
$SCDIR/carvel-deploy-package.sh $APP_NAME $PACKAGE_NAME $PACKAGE_VERSION "./scdf-values.yml" "$NS"
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Deployed SCDF in ${bold}$elapsed${end} seconds"
