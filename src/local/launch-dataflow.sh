#!/usr/bin/env bash

function usage_msg() {
    echo "Arguments: [--skipper-compose | --no-skipper] [--pro] [--no-dataflow]"
    echo "where:"
    echo "  --skipper-compose: Launches skipper container"
    echo "  --no-skipper: Doesn't launch skipper container or cli"
    echo "  --no-dataflow: Don't start Data Flow"
    echo "  --pro: Launches SCDF Pro instead of OSS"
}

SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)

PLATFORM_TYPE=local

COMPOSE_PROFILE=
SKIPPER_CLI=true
USE_PRO=false
START_DATAFLOW=true
while [ "$1" != "" ]; do
    case $1 in
    "--skipper-compose")
        COMPOSE_PROFILE="--profile skipper"
        SKIPPER_CLI=false
        ;;
    "--no-skipper")
        COMPOSE_PROFILE=
        SKIPPER_CLI=false
        ;;
    "--pro")
        USE_PRO=true
        ;;
    "--no-dataflow")
        START_DATAFLOW=false
        ;;
    *)
        usage_msg
        exit 1
        ;;
    esac
    shift
done

echo "docker compose -f "$SCDIR/docker-compose.yml" up $COMPOSE_PROFILE"
docker-compose -f "$SCDIR/docker-compose.yml" $COMPOSE_PROFILE up &
echo "Waiting for RabbitMQ"
wget --retry-connrefused --read-timeout=20 --timeout=15 --tries=10 --continue -q http://localhost:5672
echo "Waiting for MariaDB"
wget --retry-connrefused --read-timeout=20 --timeout=15 --tries=10 --continue -q http://localhost:3306

if [ "$SKIPPER_CLI" == "true" ]; then
    echo "java -jar skipper"
    java -jar $SCDIR/../../spring-cloud-skipper/spring-cloud-skipper-server/target/spring-cloud-skipper-server-2.11.0-SNAPSHOT.jar \
        --spring.datasource.url='jdbc:mariadb://localhost:3306/dataflow' \
        --spring.datasource.username=spring \
        --spring.datasource.password=spring \
        --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver &
fi
if [ "$COMPOSE_PROFILE" == "skipper" ] || [ "$SKIPPER_CLI" == "true" ]; then
    echo "Waiting for Skipper"
    wget --retry-connrefused --read-timeout=20 --timeout=15 --tries=10 --continue -q http://localhost:7577
    echo "Skipper running"
fi

read -p "Press any key to launch Spring Cloud Data Flow..."
if [ "$START_DATAFLOW" == "true" ]; then
    if [ "$USE_PRO" == "true" ]; then
        java -jar $SCDIR/../../../scdf-pro/scdf-pro-server/target/scdf-pro-server-1.6.0-SNAPSHOT.jar \
            --spring.datasource.url='jdbc:mariadb://localhost:3306/dataflow' \
            --spring.datasource.username=spring \
            --spring.datasource.password=spring \
            --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
    else
        java -jar $SCDIR/../../spring-cloud-dataflow-server/target/spring-cloud-dataflow-server-2.11.0-SNAPSHOT.jar \
            --spring.datasource.url='jdbc:mariadb://localhost:3306/dataflow' \
            --spring.datasource.username=spring \
            --spring.datasource.password=spring \
            --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
    fi
else
    echo "You may start $(realpath $SCDIR/../../spring-cloud-dataflow-server/target/spring-cloud-dataflow-server-2.11.0-SNAPSHOT.jar)"
fi
