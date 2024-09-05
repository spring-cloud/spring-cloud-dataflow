#!/bin/bash

function create_and_clear() {
    rm -rf "$1"
    mkdir -p "$1"
}

SCDIR=$(realpath $(dirname "$(readlink -f "${BASH_SOURCE[0]}")"))
set -euxo pipefail
pushd $SCDIR > /dev/null
export DATAFLOW_VERSION=$(./mvnw help:evaluate -o -Dexpression=project.version -q -DforceStdout)
export SKIPPER_VERSION=$(./mvnw help:evaluate -o -Dexpression=spring-cloud-skipper.version -pl spring-cloud-dataflow-parent -q -DforceStdout)

if [ "$PACKAGE_VERSION" = "" ]; then
  export PACKAGE_VERSION=$DATAFLOW_VERSION
fi

# you can launch a local docker registry using docker run -d -p 5000:5000 --name registry registry:2.7
# export REPO_PREFIX="<local-machine-ip>:5000/"
if [ "$REPO_PREFIX" = "" ]; then
  REPO_PREFIX="docker.io/"
fi

export PACKAGE_BUNDLE_REPOSITORY="${REPO_PREFIX}springcloud/scdf-oss-package"
export REPOSITORY_BUNDLE="${REPO_PREFIX}springcloud/scdf-oss-repo"

export SKIPPER_REPOSITORY="springcloud/spring-cloud-skipper-server"
export SERVER_REPOSITORY="springcloud/spring-cloud-dataflow-server"
export CTR_VERSION=$DATAFLOW_VERSION
export PACKAGE_NAME="scdf"
export PACKAGE_BUNDLE_TEMPLATE="src/carvel/templates/bundle/package"
export IMGPKG_LOCK_TEMPLATE="src/carvel/templates/imgpkg"
export VENDIR_SRC_IN="src/carvel/config"
export SERVER_VERSION="$DATAFLOW_VERSION"

export PACKAGE_BUNDLE_GENERATED=/tmp/generated/packagebundle
export IMGPKG_LOCK_GENERATED_IN=/tmp/generated/imgpkgin
export IMGPKG_LOCK_GENERATED_OUT=/tmp/generated/imgpkgout
create_and_clear $PACKAGE_BUNDLE_GENERATED
create_and_clear $IMGPKG_LOCK_GENERATED_IN
create_and_clear $IMGPKG_LOCK_GENERATED_OUT

echo "bundle-path=$PACKAGE_BUNDLE_GENERATED"
export SCDF_DIR="$SCDIR"

sh "$SCDIR/.github/actions/build-package-bundle/build-package-bundle.sh"

imgpkg push --bundle "$PACKAGE_BUNDLE_REPOSITORY:$PACKAGE_VERSION" --file "$PACKAGE_BUNDLE_GENERATED"

export REPO_BUNDLE_TEMPLATE="src/carvel/templates/bundle/repo"

export REPO_BUNDLE_RENDERED=/tmp/generated/reporendered
export REPO_BUNDLE_GENERATED=/tmp/generated/repobundle
create_and_clear $REPO_BUNDLE_RENDERED
create_and_clear $REPO_BUNDLE_GENERATED

sh "$SCDIR/.github/actions/build-repository-bundle/build-repository-bundle.sh"

imgpkg push --bundle "$REPOSITORY_BUNDLE:$PACKAGE_VERSION" --file "$REPO_BUNDLE_GENERATED"

popd
