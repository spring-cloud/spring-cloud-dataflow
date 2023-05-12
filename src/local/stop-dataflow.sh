#!/usr/bin/env bash

SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

PLATFORM_TYPE=local

echo "Stopping Dataflow Server"
set +e
curl -X POST http://localhost:9393/actuator/shutdown
echo "Stopping Skipper"
curl -X POST http://localhost:7577/actuator/shutdown

echo "Stopping RabbitMQ and MariaDB in docker compose"
docker-compose -f "$SCDIR/docker-compose.yml" stop
docker-compose -f "$SCDIR/docker-compose.yml" rm --force
