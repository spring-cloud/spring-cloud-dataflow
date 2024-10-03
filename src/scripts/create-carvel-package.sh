#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ "$SCDF_TYPE" = "" ]; then
    export SCDF_TYPE=oss
fi
if [ "$TARGET_DIR" != "" ]; then
    pushd $TARGET_DIR
fi
if [ "$VERSION" = "" ] || [ "$SKIPPER_VERSION" = "" ]; then
    ./mvnw -s .settings.xml help:evaluate -Dexpression=project.version > /dev/null
fi
if [ "$VERSION" = "" ]; then
    export VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
fi
if [ "$DATAFLOW_VERSION" = "" ]; then
    export DATAFLOW_VERSION=$VERSION
fi
if [ "$PACKAGE_VERSION" = "" ]; then
    export PACKAGE_VERSION=$DATAFLOW_VERSION
fi
if [ "$SKIPPER_VERSION" = "" ]; then
    export SKIPPER_VERSION=$(./mvnw help:evaluate -Dexpression=spring-cloud-skipper.version -pl spring-cloud-dataflow-parent -o -q -DforceStdout)
fi
if [ "$TARGET_DIR" != "" ]; then
    popd
fi
export SERVER_VERSION=$DATAFLOW_VERSION
if [ "$SCDF_TYPE" = "pro" ]; then
    export SERVER_REPOSITORY="spring-scdf-docker-dev-local.usw1.packages.broadcom.com/p-scdf-for-kubernetes/scdf-pro-server"
else
    export SERVER_REPOSITORY="springcloud/spring-cloud-dataflow-server"
fi
export CTR_VERSION=$DATAFLOW_VERSION
export PACKAGE_NAME=scdf
export PACKAGE_BUNDLE_TEMPLATE="src/carvel/templates/bundle/package"
export VENDIR_SRC_IN="src/carvel/config"
export IMGPKG_LOCK_TEMPLATE="src/carvel/templates/imgpkg"

echo "Project Version=$PACKAGE_VERSION"
echo "Data Flow Version=$DATAFLOW_VERSION"
echo "Skipper Version=$SKIPPER_VERSION"
echo "Build Type=$SCDF_TYPE"

pushd src/carvel || exit
npm install
npm ci
npm run format-check
popd || exit

source "$SCDIR/.github/actions/build-package-bundle/build-package-bundle.sh"

if [ "$1" != "no-push" ]; then
    if [ "$REGISTRY" = "" ]; then
        REGISTRY=springcloud
    fi

    echo "Bundle path:$PACKAGE_BUNDLE_GENERATED"
    REPOSITORY="$REGISTRY/scdf-$SCDF_TYPE-package"

    imgpkg push --bundle "$REPOSITORY:$PACKAGE_VERSION" --file "$PACKAGE_BUNDLE_GENERATED" --registry-username "$DOCKER_HUB_USERNAME" --registry-password "$DOCKER_HUB_PASSWORD"
    docker pull "$REPOSITORY:$PACKAGE_VERSION"

    export REPO_BUNDLE_TEMPLATE="src/carvel/templates/bundle/repo"
    if [ "$PACKAGE_BUNDLE_REPOSITORY" = "" ]; then
        export PACKAGE_BUNDLE_REPOSITORY="$REPOSITORY"
    fi
    source "$SCDIR/.github/actions/build-repository-bundle/build-repository-bundle.sh"

    echo "Repository path: $REPO_BUNDLE_GENERATED"
    REPOSITORY="$REGISTRY/scdf-$SCDF_TYPE-repo"
    imgpkg push --bundle "$REPOSITORY:$PACKAGE_VERSION" --file "$REPO_BUNDLE_GENERATED" --registry-username "$DOCKER_HUB_USERNAME" --registry-password "$DOCKER_HUB_PASSWORD"
    docker pull "$REPOSITORY:$PACKAGE_VERSION"
fi
exit $?
