#!/bin/bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
ROOTDIR=$(realpath "$SCDIR/../../..")
set -e
pushd "$ROOTDIR/../spring-cloud-dataflow-samples/restaurant-stream-apps"  > /dev/null
pushd scdf-app-kitchen  > /dev/null
./mvnw install spring-boot:build-image -DskipTests -Dspring-boot.build-image.pullPolicy=IfNotPresent -Dspring-boot.build-image.imageName=springcloudstream/scdf-app-kitchen:1.0.0-SNAPSHOT  -B --no-transfer-progress
popd > /dev/null
pushd scdf-app-customer > /dev/null
./mvnw install spring-boot:build-image -DskipTests -Dspring-boot.build-image.pullPolicy=IfNotPresent -Dspring-boot.build-image.imageName=springcloudstream/scdf-app-customer:1.0.0-SNAPSHOT  -B --no-transfer-progress
popd > /dev/null
pushd scdf-app-waitron > /dev/null
./mvnw install spring-boot:build-image -DskipTests -Dspring-boot.build-image.pullPolicy=IfNotPresent -Dspring-boot.build-image.imageName=springcloudstream/scdf-app-waitron:1.0.0-SNAPSHOT  -B --no-transfer-progress
popd > /dev/null
