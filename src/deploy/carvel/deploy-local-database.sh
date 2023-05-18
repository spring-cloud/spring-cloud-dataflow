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
kubectl rollout status deployment --namespace "$DATABASE" $DATABASE
set +e
JDBC_URL="jdbc:$DATABASE://$DATABASE.$DATABASE/dataflow"
$SCDIR/configure-database.sh dataflow $DATABASE "$JDBC_URL" $DATABASE database-username database-password
$SCDIR/configure-database.sh skipper $DATABASE "$JDBC_URL" $DATABASE database-username database-password
"$SCDIR/carvel-import-secret.sh" "$DATABASE" "$NS" "$DATABASE"
export DATABASE
echo "Deployed $DATABASE. Host:$DATABASE.$DATABASE"
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo -e "Deployed $DATABASE in ${bold}$elapsed${end} seconds"
