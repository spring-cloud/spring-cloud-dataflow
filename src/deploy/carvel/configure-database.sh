#!/usr/bin/env bash
if [ -z "$BASH_VERSION" ]; then
    echo "This script requires Bash. Use: bash $0 $*"
    exit 1
fi
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
if [ "$DEBUG" == "true" ]; then
    echo "DEBUG: configure-database.sh $*"
fi
if [ "$4" = "" ]; then
    echo "<app> <database> <url> <username> <password>"
    echo " OR"
    echo "<app> <database> <url> <secret-name> [secret-username-key] [secret-password-key]"
    echo " - secret-username-key: key name in secret. The default is username"
    echo " - secret-password-key: key name in secret. The default is password"
    echo "  If username / password is provided it will be assigned to the values file."
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
"oracle")
    DATABASE=oracle
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
"oracle")
    JDBC_DRIVER_CLASS=oracle.jdbc.OracleDriver
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

if [ "$DIALECT" = "" ] && [ "$DATABASE" = "mariadb" ]; then
    DIALECT="org.hibernate.dialect.MariaDB106Dialect"
fi
if [ "$DIALECT" != "" ]; then
    if [ "$DEBUG" == "true" ]; then
        echo "DEBUG: DIALECT=$DIALECT"
    fi
    yq ".scdf.${APP}.database.dialect=\"$DIALECT\"" -i ./scdf-values.yml
fi
if [ "$6" != "" ]; then
    SECRET_NAME=$4
    SECRET_USERNAME_KEY="$5"
    SECRET_PASSWORD_KEY="$6"
elif [ "$5" != "" ]; then
    USERNAME="$4"
    PASSWORD="$5"
else
    SECRET_NAME=$4
    SECRET_USERNAME_KEY=username
    SECRET_PASSWORD_KEY=password
fi
if [ "$SECRET_NAME" != "" ]; then
    if [ "$DEBUG" == "true" ]; then
        echo "DEBUG: SECRET_NAME=$SECRET_NAME, SECRET_USERNAME_KEY=$SECRET_USERNAME_KEY, SECRET_PASSWORD_KEY=$SECRET_PASSWORD_KEY"
    fi
    yq ".scdf.${APP}.database.secretName=\"$SECRET_NAME\"" -i ./scdf-values.yml
    yq ".scdf.${APP}.database.secretUsernameKey=\"$SECRET_USERNAME_KEY\"" -i ./scdf-values.yml
    yq ".scdf.${APP}.database.secretPasswordKey=\"$SECRET_PASSWORD_KEY\"" -i ./scdf-values.yml
else
    if [ "$USERNAME" = "" ]; then
        echo "Expected $USERNAME"
        exit 1
    fi
    if [ "$PASSWORD" = "" ]; then
        echo "Expected $PASSWORD"
        exit 1
    fi
    yq ".scdf.${APP}.database.username=\"$USERNAME\"" -i ./scdf-values.yml
    yq ".scdf.${APP}.database.password=\"$PASSWORD\"" -i ./scdf-values.yml
fi

echo "Set ${APP} JDBC url: $JDBC_URL"
echo "Set ${APP} JDBC class: $JDBC_DRIVER_CLASS"
echo "Configured ${APP} $DATABASE"
