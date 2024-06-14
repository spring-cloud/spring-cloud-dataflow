#!/usr/bin/env bash
if [ -n "$BASH_SOURCE" ]; then
  SCDIR="$(readlink -f "${BASH_SOURCE[0]}")"
elif [ -n "$ZSH_VERSION" ]; then
  setopt function_argzero
  SCDIR="${(%):-%N}"
elif eval '[[ -n ${.sh.file} ]]' 2>/dev/null; then
  eval 'SCDIR=${.sh.file}'
else
  echo 1>&2 "Unsupported shell. Please use bash, ksh93 or zsh."
    exit 2
fi
SCDIR="$(dirname "$SCDIR")"
ROOTDIR="$(realpath "$SCDIR/..")"
MVNW="$ROOTDIR/mvnw"
pushd "$ROOTDIR" > /dev/null
if [ "$PACKAGE_VERSION" = "" ]; then
    $MVNW help:evaluate -Dexpression=project.version -q -DforceStdout > /dev/null
    PACKAGE_VERSION=$($MVNW help:evaluate -Dexpression=project.version -q -DforceStdout)
    if [[ "$PACKAGE_VERSION" = *"Downloading"* ]]; then
        PACKAGE_VERSION=$($MVNW help:evaluate -Dexpression=project.version -q -DforceStdout)
    fi
fi
echo "PACKAGE_VERSION=$PACKAGE_VERSION"
if [[ "$PACKAGE_VERSION" != *"SNAPSHOT"* ]]; then
    yq '.default.version="release"' -i "$SCDIR/../src/deploy/versions.yaml"
    echo "Setting default.version=release, default.package-version=$PACKAGE_VERSION"
    yq ".default.package-version=\"$PACKAGE_VERSION\"" -i "$SCDIR/../src/deploy/versions.yaml"
    echo "Setting scdf-type.oss.release=$PACKAGE_VERSION"
    yq ".scdf-type.oss.release=\"$PACKAGE_VERSION\"" -i "$SCDIR/../src/deploy/versions.yaml"
fi
popd