#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
export SCDIR
PLATFORM_TYPE=local
DC_ARGS="-f $SCDIR/docker-compose.yml"
if [ "$1" == "keycloak" ] || [ "$1" == "oauth" ]; then
  DC_ARGS="$DC_ARGS -f $SCDIR/docker-compose-keycloak.yml --profile keycloak"
fi
set +e
echo "Stopping Dataflow Server"
curl -X POST http://localhost:9393/actuator/shutdown
if [ -f dataflow.pid ]; then
    kill -9 "$(cat dataflow.pid)"
    rm -f dataflow.pid
fi
ps -ef | grep java | grep dataflow
echo "Stopping Skipper"
curl -X POST http://localhost:7577/actuator/shutdown
if [ -f skipper.pid ]; then
    kill -9 "$(cat skipper.pid)"
    rm -f skipper.pid
fi
ps -ef | grep java | grep skipper
echo "Stopping $DC_ARGS"
docker compose $DC_ARGS down
docker compose $DC_ARGS rm --force
