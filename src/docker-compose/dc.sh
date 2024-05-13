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

if [ "$1" == "" ]; then
    echo "Usage is: "${BASH_SOURCE[0]}" <database> <broker> [compose-command] [options] [flags] [--no-wait]"
    echo "Where:"
    echo "    database: is one of mariadb, mysql, mssql or postgres"
    echo "    broker: is on of kafka or rabbitmq"
    echo "    compose-command: One of up,down,rm,kill,stop,start,run,restart,pull,pause,create,build. Default is up."
    echo "    options: one or more of ssl, zipkin, prometheus, influxdb, wavefront, ssl, dood, debug-dataflow, debug-skipper"
    echo "    flags: any docker-compose options"
    echo "    --wait: Wait for input before starting."
    echo "  This will invoke docker-compose with all the relevant files and provided options as well as the 'up' command"
    exit 1
fi
BROKER=rabbitmq
DATABASE=postgres
ARGS=
DC_OPTS=
DC_CMD=
WAIT=false
while [ "$1" != "" ]; do
    case $1 in
    "rabbit" | "rabbitmq")
        BROKER=rabbitmq
        ;;
    "kafka")
        BROKER=kafka
        ;;
    "mysql")
        DATABASE=mysql
        ;;
    "mssql")
        DATABASE=mssql
        ;;
    "maria" | "mariadb")
        DATABASE=mariadb
        ;;
    "postgres" | "postgresql")
        DATABASE=postgres
        ;;
    "--wait")
        WAIT=true
        ;;
    "down" | "up" | "rm" | "kill" | "stop" | "start" | "run" | "restart" | "pull" | "pause" | "create" | "build")
        if [ "$DC_CMD" != "" ];then
            echo "Only one command allowed not $DC_CMD and $1"
            exit 1
        fi
        DC_CMD=$1
        ;;
    *)
        if [ -f "$SCDIR/docker-compose-$1.yml" ]; then
            if [ "$ARGS" != "" ]; then
                ARGS="$ARGS -f $SCDIR/docker-compose-$1.yml"
            else
                ARGS="-f $SCDIR/docker-compose-$1.yml"
            fi
        else
            if [ "$DC_OPTS" == "" ]; then
                DC_OPTS="$1"
            else
                DC_OPTS="$DC_OPTS $1"
            fi
        fi
        ;;
    esac
    shift
done
if [ "$BROKER" == "" ]; then
    echo "Provide a broker name like kafka or rabbitmq"
    exit 1
fi
if [ "$DATABASE" == "" ]; then
    echo "Provide a database name like mysql, mariadb or postgres"
    exit 1
fi
if [ "$DC_CMD" = "" ]; then
    DC_CMD=up
fi
set +e
docker-compose -v 2&> /dev/null
RC=$?
if ((RC==0)); then
    DC="docker-compose"
else
    DC="docker compose"
fi
BASIC_ARGS="-f $SCDIR/docker-compose.yml -f $SCDIR/docker-compose-$BROKER.yml -f $SCDIR/docker-compose-$DATABASE.yml"
if [ "$ARGS" != "" ]; then
    ARGS="$ARGS $BASIC_ARGS"
else
    ARGS="$BASIC_ARGS"
fi
echo "Invoking:$DC $ARGS $DC_CMD $DC_OPTS"
if [ "$WAIT" == "true" ]; then
    echo "Press any key to continue..."
    read -s -n 1
fi
$DC -f "$SCDIR/docker-compose.yml" $ARGS $DC_CMD $DC_OPTS
