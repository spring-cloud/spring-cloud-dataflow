#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
start_time=$(date +%s)
if [ "$1" = "" ]; then
    echo "<database> must be provided. Choose one of postgresql or mariadb"
    exit 1
fi
case $1 in
"postgresql" | "postgres")
    DATABASE=postgresql
    ;;
"mariadb" | "maria")
    DATABASE=mariadb
    ;;
*)
    echo "Unsupported or invalid database $1"
    exit 1
    ;;
esac

K8S=$(realpath $SCDIR/../../kubernetes)
set +e
$SCDIR/prepare-local-namespace.sh "$DATABASE-sa" $DATABASE
kubectl create --namespace $DATABASE -f $K8S/$DATABASE/
set -e
"$SCDIR/carvel-import-secret.sh" "$DATABASE" "$NS" "$DATABASE"

kubectl rollout status deployment --namespace "$DATABASE" $DATABASE
export DATABASE
echo "Deployed $DATABASE. Host:$DATABASE.$DATABASE"

JDBC_URL="jdbc:$DATABASE://$DATABASE.$DATABASE/dataflow"

yq ".scdf.server.database.url=\"$JDBC_URL\"" -i ./scdf-values.yml
yq ".scdf.skipper.database.url=\"$JDBC_URL\"" -i ./scdf-values.yml

case $DATABASE in
"mariadb")
    JDBC_DRIVER_CLASS=org.mariadb.jdbc.Driver
    ;;
"postgresql")
    JDBC_DRIVER_CLASS=org.postgresql.Driver
    ;;
*)
    echo "Unsupported $DATABASE."
esac
if [ "$JDBC_DRIVER_CLASS" != "" ]; then
    yq ".scdf.server.database.driverClassName=\"$JDBC_DRIVER_CLASS\"" -i ./scdf-values.yml
    yq ".scdf.skipper.database.driverClassName=\"$JDBC_DRIVER_CLASS\"" -i ./scdf-values.yml
fi

yq ".scdf.server.database.secretName=\"$DATABASE\"" -i ./scdf-values.yml
yq ".scdf.server.database.secretUsernameKey=\"database-username\"" -i ./scdf-values.yml
yq ".scdf.server.database.secretPasswordKey=\"database-password\"" -i ./scdf-values.yml

yq ".scdf.skipper.database.secretName=\"$DATABASE\"" -i ./scdf-values.yml
yq ".scdf.skipper.database.secretUsernameKey=\"database-username\"" -i ./scdf-values.yml
yq ".scdf.skipper.database.secretPasswordKey=\"database-password\"" -i ./scdf-values.yml

echo "Set JDBC url: $JDBC_URL"
echo "Set JDBC class: $JDBC_DRIVER_CLASS"
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Deployed $DATABASE in ${bold}$elapsed${end} seconds"
