#!/usr/bin/env bash
PLATFORM_TYPE=local
USE_SKIPPER=skipper
USE_PRO=true
docker-compose -f src/local/docker-compose.yml up $USE_SKIPPER
if [ "$USE_SKIPPER" == "" ]
then
  java -jar ../spring-cloud-skipper/spring-cloud-skipper-server/target/spring-cloud-skipper-server-2.9.0-SNAPSHOT.jar \
      --spring.datasource.url='jdbc:mariadb://localhost:3306/dataflow' \
      --spring.datasource.username=spring \
      --spring.datasource.password=spring \
      --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
fi
if [ "$USE_PRO" == "true" ]
then
  java -jar ..//scdf-pro/scdf-pro-server/target/scdf-pro-server-1.5.0-SNAPSHOT.jar \
      --spring.datasource.url='jdbc:mariadb://localhost:3306/dataflow' \
      --spring.datasource.username=spring \
      --spring.datasource.password=spring \
      --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
else
  java -jar ./spring-cloud-dataflow-server/target/spring-cloud-dataflow-server-2.10.0-SNAPSHOT.jar \
    --spring.datasource.url='jdbc:mariadb://localhost:3306/dataflow' \
    --spring.datasource.username=spring \
    --spring.datasource.password=spring \
    --spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
fi
