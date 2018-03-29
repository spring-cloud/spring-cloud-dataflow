#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
pushd custom-apps/skipper-server-with-drivers101 > /dev/null
./gradlew clean build install -Dmaven.repo.local=${repository}
popd > /dev/null
popd > /dev/null
