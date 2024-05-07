#!/usr/bin/env bash
DB=$1
case $DB in

    "mariadb" | "postgres")
        echo "Executing database integration test for $DB"
        ;;

    *)
        if [ "$DB" == "" ]; then
            echo "Database type required. One of mariadb, postgres"
        else
            echo "Invalid database $DB for integration test"
        fi
        exit 1
        ;;
esac

./mvnw -s .settings.xml -pl spring-cloud-dataflow-server -Dgroups=$DB -Pfailsafe -B integration-test verify