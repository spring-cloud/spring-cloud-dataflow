#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

PLATFORM_TYPE=local

echo "Stopping Dataflow Server"
if [ -f dataflow.pid ]; then
    kill "$(cat dataflow.pid)"
    rm -f dataflow.pid
fi
set +e
curl -X POST http://localhost:9393/actuator/shutdown
echo "Stopping Skipper"
if [ -f skipper.pid ]; then
    kill "$(cat skipper.pid)"
    rm -f skipper.pid
fi
curl -X POST http://localhost:7577/actuator/shutdown

echo "Stopping RabbitMQ and MariaDB in docker compose"
docker-compose -f "$SCDIR/docker-compose.yml" stop
docker-compose -f "$SCDIR/docker-compose.yml" rm --force
