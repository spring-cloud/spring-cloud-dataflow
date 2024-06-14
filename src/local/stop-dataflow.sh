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
