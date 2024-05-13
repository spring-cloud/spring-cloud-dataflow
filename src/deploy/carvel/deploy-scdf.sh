#!/usr/bin/env bash
if [ -n "$BASH_SOURCE" ]; then
  SCDIR="$(readlink -f "${BASH_SOURCE[0]}")"
elif [ -n "$ZSH_VERSION" ]; then
  setopt function_argzero
  SCDIR="${(%):-%N}"
elif eval '[[ -n ${.sh.file} ]]' 2>/dev/null; then
  eval 'SCDIR=${.sh.file}'
else
  echo 1>&2 "Unsupported shell. Please use bash, ksh93 or zsh."
  exit 2
fi
SCDIR="$(dirname "$SCDIR")"

bold="\033[1m"
dim="\033[2m"
end="\033[0m"
function check_env() {
  eval ev='$'$1
  if [ "$ev" = "" ]; then
    echo "env var $1 not defined"
    if ((sourced != 0)); then
      return 1
    else
      exit 1
    fi
  fi
}
if [ "$SCDF_TYPE" = "" ]; then
    echo "Environmental variable SCDF_TYPE must be set to one of oss or pro."
fi
check_env NS
check_env PACKAGE_VERSION
check_env SCDF_TYPE

start_time=$(date +%s)
# the following names are your choice.

COUNT=$(kubectl get namespace $NS | grep -c "$NS")
if ((COUNT = 0)); then
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
SERVER_TAG=$(yq '.scdf.server.image.tag' ./scdf-values.yml)
if [ "$DATAFLOW_VERSION" != "" ]; then
    if [ "$SCDF_TYPE" = "oss" ]; then
        if [ "$SERVER_TAG" = "null" ] || [ "$SERVER_TAG" = "" ]; then
            yq ".scdf.server.image.tag=\"$DATAFLOW_VERSION\"" -i ./scdf-values.yml
            echo "Overriding Data Flow version=$DATAFLOW_VERSION"
        else
            echo "Using Data Flow version=$SERVER_TAG"
        fi
    fi
    CTR_TAG=$(yq '.scdf.ctr.image.tag' ./scdf-values.yml)
    if [ "$CTR_TAG" = "null" ] || [ "$CTR_TAG" = "" ]; then
        yq ".scdf.ctr.image.tag=\"$DATAFLOW_VERSION\"" -i ./scdf-values.yml
        echo "Overriding Composed Task Runner version=$DATAFLOW_VERSION"
    else
        echo "Using Composed Task Runner version=$CTR_TAG"
    fi
else
    if [ "$SCDF_TYPE" = "oss" ] && [ "$SERVER_TAG" != "null" ] && [ "$SERVER_TAG" != "" ]; then
        echo "Using Data Flow version=$SERVER_TAG"
    fi
fi
SERVER_TAG=$(yq '.scdf.server.image.tag' ./scdf-values.yml)
if [ "$DATAFLOW_PRO_VERSION" != "" ] && [ "$SCDF_TYPE" = "pro" ]; then
    if [ "$SERVER_TAG" = "null" ] || [ "$SERVER_TAG" = "" ]; then
        yq ".scdf.server.image.tag=\"$DATAFLOW_PRO_VERSION\"" -i ./scdf-values.yml
        echo "Overriding Data Flow Pro version=$DATAFLOW_PRO_VERSION"
    else
        echo "Using Data Flow Pro version=$SERVER_TAG"
    fi
else
    if [ "$SERVER_TAG" != "null" ] && [ "$SERVER_TAG" != "" ]; then
        echo "Using Data Flow Pro version=$SERVER_TAG"
    fi
fi
SKIPPER_TAG=$(yq '.scdf.skipper.image.tag' ./scdf-values.yml)
if [ "$SKIPPER_VERSION" != "" ]; then
    if [ "$SKIPPER_TAG" = "null" ] || [ "$SKIPPER_TAG" = "" ]; then
        yq ".scdf.skipper.image.tag=\"$SKIPPER_VERSION\"" -i ./scdf-values.yml
        echo "Overriding Skipper version=$SKIPPER_VERSION"
    else
        echo "Using Skipper version=$SKIPPER_TAG"
    fi
else
    if [ "$SKIPPER_TAG" != "null" ] && [ "$SKIPPER_TAG" != "" ]; then
        echo "Using Skipper version=$SKIPPER_TAG"
    fi
fi

"$SCDIR/carvel-import-secret.sh" "scdfmetadata" "$NS"
"$SCDIR/carvel-import-secret.sh" "reg-creds-dockerhub" "$NS"

if [ "$SCDF_TYPE" = "pro" ]; then
    "$SCDIR/carvel-import-secret.sh" "reg-creds-dev-registry" "$NS"
fi

"$SCDIR/carvel-import-secret.sh" "scdfmetadata" "$NS"
"$SCDIR/carvel-import-secret.sh" "reg-creds-dockerhub" "$NS"

if [ "$SCDF_TYPE" = "pro" ]; then
    "$SCDIR/carvel-import-secret.sh" "reg-creds-dev-registry" "$NS"
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
