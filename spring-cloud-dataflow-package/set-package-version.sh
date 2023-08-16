#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
MVNW=$SCDIR/../mvnw
if [ "$PACKAGE_VERSION" = "" ]; then
    PACKAGE_VERSION=$($MVNW help:evaluate -Dexpression=project.version -q -DforceStdout)
fi
echo "PACKAGE_VERSION=$PACKAGE_VERSION"
if [[ "$PACKAGE_VERSION" != *"SNAPSHOT"* ]]; then
    yq '.default.version="release"' -i "$SCDIR/../src/deploy/versions.yaml"
    echo "Setting default.version=release $PACKAGE_VERSION"
    yq ".default.package-version=\"$PACKAGE_VERSION\"" -i "$SCDIR/../src/deploy/versions.yaml"
fi
