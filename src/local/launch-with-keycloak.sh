#!/bin/bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
PROJECT_DIR=$(realpath "$SCDIR/../..")

function usage_msg() {
    echo "Arguments: [--pro]"
    echo "where:"
    echo "  --pro: Launches SCDF Pro instead of OSS"
}
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 0
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
export SCDIR
PROJECT_DIR=$(realpath "$SCDIR/../..")
if [ "$DATAFLOW_VERSION" = "" ]; then
    DATAFLOW_VERSION=3.0.0-SNAPSHOT
fi
export PLATFORM_TYPE=local
USE_PRO=false
RUN_SKIPPER=false
while [ "$1" != "" ]; do
    case $1 in
    "--pro")
        USE_PRO=true
        ;;
    "--skipper")
      RUN_SKIPPER=true
      ;;
    *)
        usage_msg
        exit 1
        ;;
    esac
    shift
done
DC_OPT="-f "$SCDIR/docker-compose.yml" -f "$SCDIR/docker-compose-keycloak.yml" --profile keycloak"
echo "docker compose $DC_OPT create"
docker compose $DC_OPT create
echo "docker compose $DC_OPT start"
docker compose $DC_OPT start
echo "Waiting for RabbitMQ"
wget --retry-connrefused --read-timeout=20 --timeout=15 --tries=10 --continue -q http://localhost:5672
echo "Waiting for MariaDB"
wget --retry-connrefused --read-timeout=20 --timeout=15 --tries=10 --continue -q http://localhost:3306
echo "Waiting for Keycloak"
wget --retry-connrefused --read-timeout=20 --timeout=15 --tries=10 --continue -q http://localhost:8080

if [ "$RUN_SKIPPER" == "true" ]; then
echo "java -jar skipper"
java -jar $PROJECT_DIR/spring-cloud-skipper/spring-cloud-skipper-server/target/spring-cloud-skipper-server-$DATAFLOW_VERSION.jar \
    --spring.datasource.url='jdbc:mariadb://localhost:3306/dataflow' \
    --spring.datasource.username=spring \
    --spring.datasource.password=spring \
    --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
    --spring.jpa.database.platform=org.hibernate.dialect.MariaDB106Dialect \
    --spring.config.additional-location="file://$SCDIR/application-skipper-keycloak.yaml" | tee skipper.log &
echo "$!" > skipper.pid
echo "Waiting for Skipper"
wget --retry-connrefused --read-timeout=20 --timeout=15 --tries=10 --continue -q http://localhost:7577
echo "Skipper running"
fi

if [ "$USE_PRO" == "true" ]; then
    if [ "$DATAFLOW_PRO_VERSION" = "" ]; then
        DATAFLOW_PRO_VERSION=3.0.0-SNAPSHOT
    fi
    SCDF_JAR="$(realpath $PROJECT_DIR/../scdf-pro/scdf-pro-server/target/scdf-pro-server-$DATAFLOW_PRO_VERSION.jar)"

else
    SCDF_JAR="$(realpath $PROJECT_DIR/spring-cloud-dataflow-server/target/spring-cloud-dataflow-server-$DATAFLOW_VERSION.jar)"
fi

SCDF_ARGS="--spring.cloud.dataflow.features.streams-enabled=${RUN_SKIPPER} --spring.cloud.dataflow.features.tasks-enabled=true --spring.cloud.dataflow.features.schedules-enabled=false"
if [ "$RUN_DATAFLOW" == "true" ]; then
echo "launching $SCDF_JAR"
java -jar "$SCDF_JAR" \
  --spring.datasource.url=jdbc:mariadb://localhost:3306/dataflow \
  --spring.datasource.username=spring \
  --spring.datasource.password=spring \
  --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
  --spring.jpa.database.platform=org.hibernate.dialect.MariaDB106Dialect \
  --spring.config.additional-location="file://$SCDIR/application-dataflow-keycloak.yaml" $SCDF_ARGS | tee server.log &

echo "$!" > dataflow.pid
echo "Waiting for Data Flow Server"
wget --retry-connrefused --read-timeout=20 --timeout=15 --tries=10 --continue -q http://localhost:9393
echo "Data flow running"
fi