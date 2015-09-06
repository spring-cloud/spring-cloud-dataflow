# Spring Cloud Data Flow Admin

The `spring-cloud-dataflow-admin` subproject of `spring-cloud-dataflow` provides the REST API and UI via the executable boot-based `AdminApplication`.

Currently the REST API includes a `StreamController` and `TaskController` that interact with in-memory implementations of the `StreamDefinitionRepository` and `TaskDefinitionRepository`, respectively. The current implementation of the `ModuleRegistry` is a stub that is only aware of the `time` source, the `log` sink, and the `counter` sink. If the `cloud` profile is active, the Receptor-based `ModuleDeployer` will be instantiated and modules will run as LRPs on Lattice. Otherwise, the `LocalModuleDeployer` will be instantiated and the modules will be launched within the same process as the `AdminApplication` itself.

## Running the AdminApplication

1\. build from the spring-cloud-data root directory:

```
mvn clean package
```

2\. start the app:

```
java -jar spring-cloud-dataflow-admin/target/spring-cloud-dataflow-admin-1.0.0.BUILD-SNAPSHOT.jar
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
2015-08-05 07:36:19.723  INFO 59555 --- [hannel-adapter1] o.s.cloud.stream.module.log.LogSink      : Received: 2015-08-05 07:36:19
2015-08-05 07:36:24.678  INFO 59555 --- [hannel-adapter1] o.s.cloud.stream.module.log.LogSink      : Received: 2015-08-05 07:36:24
2015-08-05 07:36:29.678  INFO 59555 --- [hannel-adapter1] o.s.cloud.stream.module.log.LogSink      : Received: 2015-08-05 07:36:29
```

## Configuration:

### Default
Out of the box `spring-cloud-dataflow-admin` will start in singlenode mode. To configure
the Admin you can follow the configuration setup guidelines specified in the boot documentation found
[here](http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)

Note: The admin.yml containing the the defaults can be found [here](https://github.com/spring-cloud/spring-cloud-data/blob/master/spring-cloud-dataflow-admin/src/main/resources/admin.yml) 

### Spring Cloud Configuration
`spring-cloud-dataflow-admin` offers the user the ability to configure the admin using
[spring-cloud-config](http://cloud.spring.io/spring-cloud-config/spring-cloud-config.html).
And all configurations retrieved from the cloud config will take precedence over boot's
defaults enumerated above. The spring-cloud-dataflow-admin will look for the server at
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
