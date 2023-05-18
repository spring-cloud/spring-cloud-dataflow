#!/usr/bin/env bash
if [ "$DEBUG" == "true" ]; then
    echo "DEBUG: configure-database.sh $*"
fi

SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
if [ "$4" = "" ]; then
    echo "<app> <database> <url> <secret-name> [secret-username-key] [secret-password-key]"
    exit 1
fi

case $1 in
"dataflow")
    APP=server
    ;;
"skipper")
    APP=skipper
    ;;
*)
    echo "Invalid application: $1"
    exit 1
    ;;
esac
if [ "$DEBUG" == "true" ]; then
    echo "DEBUG: APP=$APP"
fi
case $2 in
"postgresql" | "postgres")
    DATABASE=postgresql
    ;;
"mariadb" | "maria")
    DATABASE=mariadb
    ;;
"mysql57")
    DATABASE=mysql57
    ;;
*)
    echo "Unsupported or invalid database $2"
    exit 1
    ;;
esac
set +e
JDBC_URL="$3"

yq ".scdf.${APP}.database.url=\"$JDBC_URL\"" -i ./scdf-values.yml

if [ "$DEBUG" == "true" ]; then
    echo "DEBUG: DATABASE=$DATABASE"
fi
case $DATABASE in
"mariadb" | "mysql57")
    JDBC_DRIVER_CLASS=org.mariadb.jdbc.Driver
    ;;
"postgresql")
    JDBC_DRIVER_CLASS=org.postgresql.Driver
    ;;
*)
    echo "Unsupported $DATABASE."
    ;;
esac

if [ "$DEBUG" == "true" ]; then
    echo "DEBUG: JDBC_DRIVER_CLASS=$JDBC_DRIVER_CLASS"
fi

if [ "$JDBC_DRIVER_CLASS" != "" ]; then
    yq ".scdf.${APP}.database.driverClassName=\"$JDBC_DRIVER_CLASS\"" -i ./scdf-values.yml
fi

SECRET_NAME=$4
if [ "$5" != "" ]; then
    SECRET_USERNAME_KEY="$5"
else
    SECRET_USERNAME_KEY=username
fi
if [ "$6" != "" ]; then
    SECRET_PASSWORD_KEY="$6"
else
    SECRET_PASSWORD_KEY=password
fi

if [ "$DEBUG" == "true" ]; then
    echo "DEBUG: SECRET_NAME=$SECRET_NAME"
fi
yq ".scdf.${APP}.database.secretName=\"$SECRET_NAME\"" -i ./scdf-values.yml
yq ".scdf.${APP}.database.secretUsernameKey=\"$SECRET_USERNAME_KEY\"" -i ./scdf-values.yml
yq ".scdf.${APP}.database.secretPasswordKey=\"$SECRET_PASSWORD_KEY\"" -i ./scdf-values.yml

FILE="$(mktemp).yml"
cat >$FILE <<EOF
apiVersion: secretgen.carvel.dev/v1alpha1
kind: SecretExport
metadata:
  name: $SECRET_NAME
  namespace: $DATABASE
spec:
  toNamespace: '*'
EOF
echo "Create SecretExport $SECRET_NAME to $NS"
cat $FILE
kubectl apply -f $FILE

echo "Set ${APP} JDBC url: $JDBC_URL"
echo "Set ${APP} JDBC class: $JDBC_DRIVER_CLASS"
echo "Configured ${APP} $DATABASE"
