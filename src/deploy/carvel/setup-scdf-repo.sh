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
    if [ "$ev" == "" ]; then
        echo "env var $1 not defined"
        exit 1
    fi
}

function count_kind() {
    jq --arg kind $1 --arg name $2 '.items | .[] | select(.kind == $kind) | .metadata | select(.name == $name) | .name' | grep -c -F "$2"
}

start_time=$(date +%s)
# the following names are your choice.
check_env NS
check_env SCDF_TYPE
check_env PACKAGE_VERSION
check_env DOCKER_HUB_USERNAME
check_env DOCKER_HUB_PASSWORD

$SCDIR/carvel-prepare-namespaces.sh $NS
# Credentials for docker.io

case $SCDF_TYPE in
"pro")
    PACKAGE_NAME=scdf-pro.tanzu.vmware.com
    if [ "$PACKAGE_REPO" = "" ]; then
        PACKAGE_REPO="dev.registry.tanzu.vmware.com/p-scdf-for-kubernetes"
    fi
    if [ "$REPO_NAME" = "" ]; then
        REPO_NAME="scdf-pro-repo"
    fi
    ;;
"oss")
    PACKAGE_NAME=scdf.tanzu.vmware.com
    if [ "$PACKAGE_REPO" = "" ]; then
        PACKAGE_REPO="index.docker.io/springcloud"
    fi
    if [ "$REPO_NAME" = "" ]; then
        REPO_NAME="scdf-oss-repo"
    fi
    ;;
*)
    echo "Invalid SCDF_TYPE=$SCDF_TYPE only pro or oss is acceptable"
    ;;
esac
if [ "$REGISTRY" != "" ]; then
    PACKAGE="$REGISTRY/$REPO_NAME:$PACKAGE_VERSION"
else
    PACKAGE="$PACKAGE_REPO/$REPO_NAME:$PACKAGE_VERSION"
fi
echo "Adding repository for $PACKAGE"
"$SCDIR/carvel-add-package.sh" "$PACKAGE" "$PACKAGE_NAME" "$NS"

end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Setup SCDF Carvel Repo in ${bold}$elapsed${end} seconds"
