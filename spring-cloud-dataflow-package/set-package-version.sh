#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
if [ "$MVN" = "" ]; then
    MVN=./mvnw
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOT=$(realpath $SCDIR/..)
if [ "$PACKAGE_VERSION" = "" ]; then
    pushd $ROOT > /dev/null
    $MVN help:evaluate -s .settings.xml -Dexpression=project.version > /dev/null
    PACKAGE_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
    popd > /dev/null
fi
echo "PACKAGE_VERSION=$PACKAGE_VERSION"
if [[ "$PACKAGE_VERSION" != *"SNAPSHOT"* ]]; then
    yq '.default.version="release"' -i "$ROOT/src/deploy/versions.yaml"
    echo "Setting default.version=release, default.package-version=$PACKAGE_VERSION"
    yq ".default.package-version=\"$PACKAGE_VERSION\"" -i "$ROOT/src/deploy/versions.yaml"
    echo "Setting scdf-type.oss.release=$PACKAGE_VERSION"
    yq ".scdf-type.oss.release=\"$PACKAGE_VERSION\"" -i "$ROOT/src/deploy/versions.yaml"
fi
