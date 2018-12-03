#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
pushd $BASE_PATH > /dev/null
pushd custom-apps/$APP_TEMPLATE > /dev/null
./gradlew clean build install -Dmaven.repo.local=${repository} -PprojectBuildVersion=$SKIPPER_VERSION -PspringCloudSkipperVersion=$SKIPPER_VERSION -PjarPostfix=$APP_VERSION
popd > /dev/null
popd > /dev/null
popd > /dev/null
