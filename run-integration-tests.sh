#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
pushd $SCDIR
./build-containers.sh
./run-integration-test.sh "mariadb,postgres,performance,oauth"

