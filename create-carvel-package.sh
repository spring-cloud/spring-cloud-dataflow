#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

export VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
export DATAFLOW_VERSION=$VERSION
export SKIPPER_VERSION=$VERSION
export SERVER_VERSION=$VERSION
export SERVER_REPOSITORY="springcloud/spring-cloud-dataflow-server"
export CTR_VERSION=$VERSION
export PACKAGE_NAME=scdf
export PACKAGE_BUNDLE_TEMPLATE="src/carvel/templates/bundle/package"
export VENDIR_SRC_IN="src/carvel/config"
export IMGPKG_LOCK_TEMPLATE="src/carvel/templates/imgpkg"
echo "Project Version=$VERSION"
source "$SCDIR/.github/actions/build-package-bundle/build-package-bundle.sh"

if [ "$REGISTRY" = "" ]; then
    REGISTRY=springcloud
fi

echo "Bundle path:$PACKAGE_BUNDLE_GENERATED"
REPOSITORY="$REGISTRY/scdf-package"
imgpkg push --bundle "$REPOSITORY:$VERSION" --file "$PACKAGE_BUNDLE_GENERATED" --registry-username "$DOCKER_HUB_USERNAME" --registry-password "$DOCKER_HUB_PASSWORD"
docker pull "$REPOSITORY:$VERSION"

export REPO_BUNDLE_TEMPLATE="src/carvel/templates/bundle/repo"
if [ "$PACKAGE_BUNDLE_REPOSITORY" = "" ]; then
    export PACKAGE_BUNDLE_REPOSITORY="$REPOSITORY"
fi
source "$SCDIR/.github/actions/build-repository-bundle/build-repository-bundle.sh"

echo "Repository path: $REPO_BUNDLE_GENERATED"
REPOSITORY="$REGISTRY/scdf-repo"
imgpkg push --bundle "$REPOSITORY:$VERSION" --file "$REPO_BUNDLE_GENERATED" --registry-username "$DOCKER_HUB_USERNAME" --registry-password "$DOCKER_HUB_PASSWORD"
docker pull "$REPOSITORY:$VERSION"

exit $?
