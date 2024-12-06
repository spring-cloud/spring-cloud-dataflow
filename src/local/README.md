# Local Development Support

The files in this folder are to support local testing and development.

These scripts are useful when you want to build the containers on a different architecture and publish to a private repo.

## `download-apps.sh`
Downloads all applications needed by `create-containers.sh` from Maven repository.

*If the timestamp of snapshots matches the download will be skipped.*

Usage: `download-apps.sh [version]`
* `version` is the dataflow-server version like `2.10.3`. Default is `2.11.3-SNAPSHOT`

## `create-containers.sh`
Creates all containers and pushes to local docker registry.

This script requires [jib-cli](https://github.com/GoogleContainerTools/jib/tree/master/jib-cli)

Usage: `create-containers.sh [version] [jre-version]`
* `version` is the dataflow-server version like `2.9.6`. Default is `2.11.3-SNAPSHOT`
* `jre-version` should be one of 11, 17. Default is 11


## Testing with Keycloak
### Build with Java 17
```shell
./mvnw clean install -am -pl :spring-cloud-dataflow-server -DskipTests -Dmaven.javadoc.skip=true -T 1C
```
### Execute:
```shell
./src/local/launch-keycloak.sh
```

### Debug
Module: `spring-cloud-dataflow-server`
Class: `org.springframework.cloud.dataflow.server.single.DataFlowServerApplication`
Arguments: `--spring.cloud.dataflow.features.streams-enabled=false --spring.cloud.dataflow.features.tasks-enabled=true --spring.cloud.dataflow.features.schedules-enabled=false --spring.config.additional-location="$ProjectFileDir$/src/local/application-dataflow-keycloak.yaml"`

### Run

java -jar spring-cloud-dataflow-server/target/spring-cloud-dataflow-server-3.0.0-SNAPSHOT.jar --spring.cloud.dataflow.features.streams-enabled=false --spring.cloud.dataflow.features.tasks-enabled=true --spring.cloud.dataflow.features.schedules-enabled=false --spring.config.additional-location="src/local/application-dataflow-keycloak.yaml" 