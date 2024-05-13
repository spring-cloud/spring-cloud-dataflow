#!/bin/bash
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
set -e
pushd "$ROOTDIR/../spring-cloud-dataflow-samples/restaurant-stream-apps"  > /dev/null
pushd scdf-app-kitchen  > /dev/null
./mvnw install spring-boot:build-image -DskipTests -Dspring-boot.build-image.pullPolicy=IfNotPresent -Dspring-boot.build-image.imageName=springcloudstream/scdf-app-kitchen:1.0.0-SNAPSHOT
popd > /dev/null
pushd scdf-app-customer > /dev/null
./mvnw install spring-boot:build-image -DskipTests -Dspring-boot.build-image.pullPolicy=IfNotPresent -Dspring-boot.build-image.imageName=springcloudstream/scdf-app-customer:1.0.0-SNAPSHOT
popd > /dev/null
pushd scdf-app-waitron > /dev/null
./mvnw install spring-boot:build-image -DskipTests -Dspring-boot.build-image.pullPolicy=IfNotPresent -Dspring-boot.build-image.imageName=springcloudstream/scdf-app-waitron:1.0.0-SNAPSHOT
popd > /dev/null
