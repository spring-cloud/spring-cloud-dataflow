#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
if [ "$1" == "" ]; then
    echo "Usage is: "${BASH_SOURCE[0]}" <database> <broker> [options] [flags]"
    echo "Where:"
    echo "    database: is one of mariadb, mysql or postgres"
    echo "    broker: is on of kafka or rabbitmq"
    echo "    options: one or more of ssl, zipkin, prometheus, influxdb, wavefront, ssl, dood, debug-dataflow, debug-skipper"
    echo "    flags: any docker-compose options"
    echo "  This will invoke docker-compose with all the relevant files and provided options as well as the 'up' command"
    exit 1
fi
BROKER=rabbitmq
DATABASE=postgres
ARGS=
DC_OPTS=
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
    "maria" | "mariadb")
        DATABASE=mariadb
        ;;
    "postgres" | "postgresql")
        DATABASE=postgres
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
if [ "$DC_OPTS" = "" ]; then
    DC_OPTS=up
fi
docker-compose -f "$SCDIR/docker-compose.yml" -f "$SCDIR/docker-compose-$BROKER.yml" -f "$SCDIR/docker-compose-$DATABASE.yml" $ARGS $DC_OPTS
