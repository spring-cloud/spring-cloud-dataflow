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

ROOTDIR=$(realpath "$SCDIR/../../..")
pushd "$ROOTDIR" > /dev/null
    $ROOTDIR/mvnw -T 0.5C -o -am -pl :spring-cloud-skipper-server install -DskipTests
    $ROOTDIR/mvnw -o -pl :spring-cloud-skipper-server spring-boot:build-image -DskipTests
popd