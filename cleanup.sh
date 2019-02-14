#!/bin/zsh
cf delete -f mminella-data-flow-server
cf delete -f mminella-skipper-server
cf delete -f timestamp-d

cd /Users/mminella/Documents/IntelliJWorkspace/spring-cloud-deployer-cloudfoundry
mvn clean install -DskipTests
cd /Users/mminella/Documents/IntelliJWorkspace/spring-cloud-data
mvn clean install -DskipTests

cf push
cf logs mminella-data-flow-server
