SPI implementation for deploying [Spring Cloud Stream](https://github.com/spring-cloud/spring-cloud-stream) modules locally, within the existing JVM process

## Running the AdminApplication

1\. build from the spring-cloud-dataflow root directory:

```
mvn clean package
```

2\. start the app:

```
java -jar spring-cloud-dataflow-admin-local/target/spring-cloud-dataflow-admin-local-1.0.0.BUILD-SNAPSHOT.jar
```

## Creating the `time | log` stream:

1\. create the 'ticktock' stream:

```
$ curl -X POST -d "name=ticktock&definition=time | log" http://localhost:9393/streams/definitions?deploy=false
```

2\. list all streams available in the repository:

```
$ curl http://localhost:9393/streams/definitions
```

3\. deploy the 'ticktock' stream:

```
$ curl -X POST http://localhost:9393/streams/deployments/ticktock
```

If successful you should see output similar to the following in the `AdminApplication` console:

```
2015-11-13 09:08:52.529  INFO 73217 --- [hannel-adapter1] log.sink   : 2015-11-13 09:08:52
2015-11-13 09:08:53.531  INFO 73217 --- [hannel-adapter1] log.sink   : 2015-11-13 09:08:53
2015-11-13 09:08:54.533  INFO 73217 --- [hannel-adapter1] log.sink   : 2015-11-13 09:08:54
```

## Configuration:

### Default
To configure the Admin you can follow the configuration setup guidelines specified in the boot documentation found
[here](http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)

Note: The admin.yml containing the the defaults can be found [here](https://github.com/spring-cloud/spring-cloud-dataflow/blob/master/spring-cloud-dataflow-admin-local/src/main/resources/admin.yml)

### Spring Cloud Configuration
The Spring Cloud Data Flow Admin offers the user the ability to configure properties via
[spring-cloud-config](http://cloud.spring.io/spring-cloud-config/spring-cloud-config.html).
And all configurations retrieved from the cloud config will take precedence over boot's
defaults enumerated above. The Spring Cloud Data Flow Admin will look for the server at
`localhost:8888`, however this can be overwritten by setting the `spring.cloud.config.uri`
property to the desired url.

#### Cloud-Config-Server configuration

To specify a repository in the cloud config server configuration.yml for the Admin,
setup a repo profile with the pattern `spring-cloud-dataflow-admin`. For example:

```YAML
spring:
  cloud:
     config:
       server:
         git:
           uri: https://github.com/myrepo/configurations
           repos:
            spring-cloud-dataflow-admin:
              pattern: spring-cloud-dataflow-admin
              uri: https://github.com/myrepo/configurations
              searchPaths: dataFlowAdmin
```

##### Fail Fast
In some cases, it may be desirable to fail startup of a service if it cannot connect to
the Config Server. If this is the desired behavior, set the bootstrap configuration
property `spring.cloud.config.failFast=true` and the client will halt with an Exception.

##### Note: 
If the Admin cannot connect to the cloud config server, the
following warning message will be logged: 
`WARN 42924 --- [           main] c.c.c.ConfigServicePropertySourceLocator : Could not locate PropertySource: I/O error on GET request for "http://localhost:8888/spring-cloud-dataflow-admin/default":Connection refused; nested exception is java.net.ConnectException: Connection refused`

To disable the cloud config server set the `spring.cloud.config.enabled` property to false.
