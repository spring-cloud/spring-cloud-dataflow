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

function usage_msg() {
    echo "Arguments: [--skipper-compose | --no-skipper] [--pro] [--no-dataflow]"
    echo "where:"
    echo "  --skipper-compose: Launches skipper container"
    echo "  --no-skipper: Doesn't launch skipper container or cli"
    echo "  --no-dataflow: Don't start Data Flow"
    echo "  --pro: Launches SCDF Pro instead of OSS"
}

PROJECT_DIR=$(realpath "$SCDIR/../..")
if [ "$DATAFLOW_VERSION" = "" ]; then
    DATAFLOW_VERSION=2.11.3-SNAPSHOT
fi
export PLATFORM_TYPE=local
COMPOSE_PROFILE=
SKIPPER_CLI=true
USE_PRO=false
START_DATAFLOW=true
NO_SKIPPER=false
while [ "$1" != "" ]; do
    case $1 in
    "--skipper-compose")
        COMPOSE_PROFILE="--profile skipper"
        SKIPPER_CLI=false
        ;;
    "--no-skipper")
        COMPOSE_PROFILE=
        SKIPPER_CLI=false
        NO_SKIPPER=true
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

if [ "$SKIPPER_CLI" = "true" ]; then
    echo "java -jar skipper"
    java -jar $PROJECT_DIR/spring-cloud-skipper/spring-cloud-skipper-server/target/spring-cloud-skipper-server-$DATAFLOW_VERSION.jar \
        --spring.datasource.url='jdbc:mariadb://localhost:3306/dataflow' \
        --spring.datasource.username=spring \
        --spring.datasource.password=spring \
        --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
        --spring.jpa.database.platform=org.hibernate.dialect.MariaDB106Dialect &
    echo "$!" > skipper.pid
fi
if [ "$COMPOSE_PROFILE" = "skipper" ] || [ "$SKIPPER_CLI" = "true" ]; then
    echo "Waiting for Skipper"
    wget --retry-connrefused --read-timeout=20 --timeout=15 --tries=10 --continue -q http://localhost:7577
    echo "Skipper running"
fi

if [ "$NO_SKIPPER" = "true" ]; then
    DATAFLOW_ARGS="$DATAFLOW_ARGS --spring.cloud.dataflow.features.streams-enabled=false"
else
    DATAFLOW_ARGS="$DATAFLOW_ARGS --spring.cloud.dataflow.features.streams-enabled=true"
fi
if [ "$NO_TASKS" = "true" ]; then
    DATAFLOW_ARGS="$DATAFLOW_ARGS --spring.cloud.dataflow.features.tasks-enabled=false"
else
    DATAFLOW_ARGS="$DATAFLOW_ARGS --spring.cloud.dataflow.features.tasks-enabled=true"
fi
if [ "$USE_PRO" = "true" ]; then
    if [ "$DATAFLOW_PRO_VERSION" = "" ]; then
        DATAFLOW_PRO_VERSION=1.6.1-SNAPSHOT
    fi
    SCDF_JAR="$(realpath $PROJECT_DIR/../scdf-pro/scdf-pro-server/target/scdf-pro-server-$DATAFLOW_PRO_VERSION.jar)"

else
    SCDF_JAR="$(realpath $PROJECT_DIR/spring-cloud-dataflow-server/target/spring-cloud-dataflow-server-$DATAFLOW_VERSION.jar)"
fi
if [[ "$DATAFLOW_ARGS" != *"--spring.datasource."* ]]; then
    DATAFLOW_ARGS="$DATAFLOW_ARGS --spring.datasource.url=jdbc:mariadb://localhost:3306/dataflow --spring.datasource.username=spring --spring.datasource.password=spring --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver --spring.jpa.database.platform=org.hibernate.dialect.MariaDB106Dialect"
fi
if [ "$START_DATAFLOW" = "true" ]; then
    echo "launching $SCDF_JAR with $DATAFLOW_ARGS"
    read -p "Press any key to launch Spring Cloud Data Flow..."
    java -jar "$SCDF_JAR" $DATAFLOW_ARGS &
    echo "$!" > dataflow.pid
else
    echo "You may execute: java -jar $SCDF_JAR $DATAFLOW_ARGS"
fi
