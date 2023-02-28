#!/usr/bin/env bash
function check_env() {
  eval ev='$'$1
  if [ "$ev" == "" ]; then
    echo "env var $1 not defined"
    exit 1
  fi
}



PACKAGE_VERSION=1.5.2-SNAPSHOT
PACKAGE_NAME=scdfpro.tanzu.vmware.com
REGISTRY=dev.registry.pivotal.io
REPO_NAME="p-scdf-for-kubernetes/scdf-pro-repo"



check_env TANZU_DOCKER_USERNAME
check_env TANZU_DOCKER_PASSWORD

docker login $REGISTRY -u $TANZU_DOCKER_USERNAME -p $TANZU_DOCKER_PASSWORD

check_env DOCKER_HUB_USERNAME
check_env DOCKER_HUB_PASSWORD

docker login index.docker.io -u $DOCKER_HUB_USERNAME -p $DOCKER_HUB_PASSWORD

check_env INTERNAL_REGISTRY
docker login $INTERNAL_REGISTRY

imgpkg copy -b $REGISTRY/$REPO_NAME:$PACKAGE_VERSION --to-tar=scdf-pro-repo-$PACKAGE_VERSION.tar
imgpkg copy --tar scdf-pro-repo-$PACKAGE_VERSION.tar --to-repo=$INTERNAL_REGISTRY/$REPO_NAME

