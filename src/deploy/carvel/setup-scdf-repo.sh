#!/usr/bin/env bash
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

SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
start_time=$(date +%s)
# the following names are your choice.
check_env NS
check_env SCDF_TYPE
check_env DOCKER_HUB_USERNAME
check_env DOCKER_HUB_PASSWORD

$SCDIR/carvel-prepare-namespaces.sh $NS
# Credentials for docker.io

$SCDIR/carvel-add-registry-secret.sh scdf-metadata-default docker.io "$DOCKER_HUB_USERNAME" "$DOCKER_HUB_PASSWORD"
$SCDIR/carvel-add-registry-secret.sh reg-creds-dockerhub docker.io "$DOCKER_HUB_USERNAME" "$DOCKER_HUB_PASSWORD"

case $SCDF_TYPE in
"pro")
    if [ "$PACKAGE_VERSION" = "" ]; then
        PACKAGE_VERSION=1.5.3-SNAPSHOT
    fi
    PACKAGE_NAME=scdfpro.tanzu.vmware.com
    REGISTRY_REPO="dev.registry.pivotal.io/p-scdf-for-kubernetes"
    REPO_NAME="scdf-pro-repo"
    ;;
"oss")
    if [ "$PACKAGE_VERSION" = "" ]; then
        PACKAGE_VERSION=2.11.0-SNAPSHOT
    fi
    PACKAGE_NAME=scdf.tanzu.vmware.com
    REGISTRY_REPO="index.docker.io/springcloud"
    REPO_NAME="scdf-oss-repo"
    ;;
*)
    echo "Invalid SCDF_TYPE=$SCDF_TYPE only pro or oss is acceptable"
    ;;
esac
if [ "$REGISTRY" != "" ]; then
    PACKAGE="$REGISTRY/$REPO_NAME:$PACKAGE_VERSION"
else
    PACKAGE="$REGISTRY_REPO/$REPO_NAME:$PACKAGE_VERSION"
    echo "Adding repository for SCDF $SCDF_TYPE: $PACKAGE_VERSION"

    if [ "$SCDF_TYPE" = "pro" ]; then
        check_env TANZU_DOCKER_USERNAME
        check_env TANZU_DOCKER_PASSWORD
        $SCDIR/carvel-add-registry-secret.sh reg-creds-dev-registry dev.registry.pivotal.io "$TANZU_DOCKER_USERNAME" "$TANZU_DOCKER_PASSWORD"
    fi
fi
$SCDIR/carvel-add-package.sh "$PACKAGE" "$PACKAGE_NAME" "$NS"
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Setup SCDF Carvel Repo in ${bold}$elapsed${end} seconds"
