#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
MVNW=$SCDIR/../mvnw
MVND=$(dirname $MVNW)
if [ "$PACKAGE_VERSION" = "" ]; then
    $MVNW help:evaluate -s $MVND/.settings.xml -Dexpression=project.version > /dev/null
    PACKAGE_VERSION=$($MVNW help:evaluate -Dexpression=project.version -o -q -DforceStdout)
fi
echo "PACKAGE_VERSION=$PACKAGE_VERSION"
if [[ "$PACKAGE_VERSION" != *"SNAPSHOT"* ]]; then
    yq '.default.version="release"' -i "$SCDIR/../src/deploy/versions.yaml"
    echo "Setting default.version=release, default.package-version=$PACKAGE_VERSION"
    yq ".default.package-version=\"$PACKAGE_VERSION\"" -i "$SCDIR/../src/deploy/versions.yaml"
    echo "Setting scdf-type.oss.release=$PACKAGE_VERSION"
    yq ".scdf-type.oss.release=\"$PACKAGE_VERSION\"" -i "$SCDIR/../src/deploy/versions.yaml"
fi
