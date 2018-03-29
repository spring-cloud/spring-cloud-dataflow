#!/bin/bash
set -e

n=0
source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository
buildversion=`date '+%Y-%m-%d-%H-%M-%S'`

pushd git-repo > /dev/null
echo $ARTIFACTORY_PASSWORD | docker login -u $ARTIFACTORY_USERNAME --password-stdin springsource-docker-private-local.jfrog.io
./gradlew clean build || n=1
tar -zc --ignore-failed-read --file ${repository}/spring-cloud-skipper-acceptance-tests-${buildversion}.tar.gz spring-cloud-skipper-acceptance-tests/build/test-docker-logs
popd > /dev/null

if [ "$n" -gt 0 ]; then
  exit $n
fi
