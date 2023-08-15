#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
MVNW=$SCDIR/../mvnw
if [ "$PACKAGE_VERSION" = "" ]; then
    PACKAGE_VERSION=$($MVNW help:evaluate -Dexpression=project.version -q -DforceStdout)
fi

if [[ "$1" == "jfrog" ]]; then
    MVNW=jfrog rt mvn
fi
echo "PACKAGE_VERSION=$PACKAGE_VERSION"
if [[ "$PACKAGE_VERSION" != *"SNAPSHOT"* ]]; then
    yq '.default.version="release"' -i "$SCDIR/../src/deploy/versions.yaml"
    echo "Setting default.version=release"
    yq ".default.package-version=\"$PACKAGE_VERSION\"" -i "$SCDIR/../src/deploy/versions.yaml"
fi
pushd "$SCDIR" || exit
$MVNW clean install -Dpackage.version="$PACKAGE_VERSION"
popd || exit
