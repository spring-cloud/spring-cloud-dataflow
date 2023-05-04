#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ "$VERSION" = "" ]; then
    export VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
fi
if [ "$PACKAGE_VERSION" = "" ]; then
    export PACKAGE_VERSION=$VERSION
fi
if [ "$DATAFLOW_VERSION" = "" ]; then
    export DATAFLOW_VERSION=$PACKAGE_VERSION
fi
if [ "$SKIPPER_VERSION" = "" ]; then
    export SKIPPER_VERSION=$(./mvnw help:evaluate -Dexpression=spring-cloud-skipper.version -q -DforceStdout)
fi
export SERVER_VERSION=$DATAFLOW_VERSION
export SERVER_REPOSITORY="springcloud/spring-cloud-dataflow-server"
export CTR_VERSION=$DATAFLOW_VERSION
export PACKAGE_NAME=scdf
export PACKAGE_BUNDLE_TEMPLATE="src/carvel/templates/bundle/package"
export VENDIR_SRC_IN="src/carvel/config"
export IMGPKG_LOCK_TEMPLATE="src/carvel/templates/imgpkg"
echo "Project Version=$PACKAGE_VERSION"
source "$SCDIR/.github/actions/build-package-bundle/build-package-bundle.sh"

if [ "$REGISTRY" = "" ]; then
    REGISTRY=springcloud
fi

echo "Bundle path:$PACKAGE_BUNDLE_GENERATED"
REPOSITORY="$REGISTRY/scdf-package"
imgpkg push --bundle "$REPOSITORY:$PACKAGE_VERSION" --file "$PACKAGE_BUNDLE_GENERATED" --registry-username "$DOCKER_HUB_USERNAME" --registry-password "$DOCKER_HUB_PASSWORD"
docker pull "$REPOSITORY:$PACKAGE_VERSION"

export REPO_BUNDLE_TEMPLATE="src/carvel/templates/bundle/repo"
if [ "$PACKAGE_BUNDLE_REPOSITORY" = "" ]; then
    export PACKAGE_BUNDLE_REPOSITORY="$REPOSITORY"
fi
source "$SCDIR/.github/actions/build-repository-bundle/build-repository-bundle.sh"

echo "Repository path: $REPO_BUNDLE_GENERATED"
REPOSITORY="$REGISTRY/scdf-repo"
imgpkg push --bundle "$REPOSITORY:$PACKAGE_VERSION" --file "$REPO_BUNDLE_GENERATED" --registry-username "$DOCKER_HUB_USERNAME" --registry-password "$DOCKER_HUB_PASSWORD"
docker pull "$REPOSITORY:$PACKAGE_VERSION"

exit $?
