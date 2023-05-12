#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
start_time=$(date +%s)
if [ "$1" != "" ]; then
    case $1 in
    "postgresql" | "postgres" | "")
        DATABASE=postgresql
        ;;
    "mariadb")
        DATABASE=mariadb
        ;;
    *)
        echo "Unsupported or invalid database $1"
        exit 1
        ;;
    esac
fi
if [ "$DATABASE" = "" ]; then
    echo "DATABASE must be define or passed as parameter to this script"
    exit 1
fi
K8S=$(realpath $SCDIR/../../kubernetes)
COUNT=$(kubectl get namespace $DATABASE | grep -c "$DATABASE")
if ((COUNT == 0)); then
    echo "Creating namespace $DATABASE"
    kubectl create namespace "$DATABASE"
else
    echo "Namespace $DATABASE exists"
fi
set +e
kubectl create --namespace $DATABASE -f $K8S/$DATABASE/
set -e
"$SCDIR/carvel-import-secret.sh" "$DATABASE" "$NS" "$DATABASE"

kubectl rollout status deployment --namespace "$DATABASE" $DATABASE
DATABASE_HOST=$(kubectl get --namespace $DATABASE services $DATABASE | grep -F $DATABASE | awk '{ print $3 }')
export DATABASE_HOST
export DATABASE
echo "Deployed $DATABASE. Host: $DATABASE_HOST"

JDBC_URL="jdbc:$DATABASE://$DATABASE.$DATABASE.svc.cluster.local/dataflow"

yq ".scdf.server.database.url=\"$JDBC_URL\"" -i ./scdf-values.yml
yq ".scdf.skipper.database.url=\"$JDBC_URL\"" -i ./scdf-values.yml
JDBC_DRIVER_CLASS=org.postgresql.Driver
if [ "$DATABASE" = "mariadb" ]; then
    JDBC_DRIVER_CLASS=org.mariadb.jdbc.Driver
fi
yq ".scdf.server.database.driverClassName=\"$JDBC_DRIVER_CLASS\"" -i ./scdf-values.yml
yq ".scdf.skipper.database.driverClassName=\"$JDBC_DRIVER_CLASS\"" -i ./scdf-values.yml
yq ".scdf.server.database.secretName=\"$DATABASE\"" -i ./scdf-values.yml
yq ".scdf.skipper.database.secretName=\"$DATABASE\"" -i ./scdf-values.yml

echo "Set JDBC url: $JDBC_URL"
echo "Set JDBC class: $JDBC_DRIVER_CLASS"
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Deployed $DATABASE in ${bold}$elapsed${end} seconds"
