# Spring Cloud Data REST API

The `spring-cloud-data-rest` subproject of `spring-cloud-data` provides the REST API via the executable boot-based `AdminApplication`.

Currently the REST API includes only a PoC version of the `StreamController` that interacts with an in-memory implementation of the `StreamDefinitionRepository` and a stubbed implementation of the `ModuleRegistry` which is only aware of the `time` source and `log` sink. If the `cloud` profile is active, the Receptor-based `ModuleDeployer` will be instantiated and modules will run as LRPs on Lattice. Otherwise, the `LocalModuleDeployer` will be instantiated and the modules will be launched within the same process as the `AdminApplication` itself.

The `StreamController` will soon be replaced by a version that supports the stream commands from the existing shell.

## Running the AdminApplication

1. build from the spring-cloud-data root directory:

```
mvn clean package
```

2. start the app:

```
java -jar spring-cloud-data-rest/target/spring-cloud-data-rest-1.0.0.BUILD-SNAPSHOT.jar
```

## Creating the `time | log` stream:

1. create the 'ticktock' stream:

```
$ curl -X POST -H "Content-Type: text/plain" -d "time | log" http://localhost:9393/streams/ticktock
```

2. list all streams available in the repository:

```
$ curl http://localhost:9393/streams
```

3. deploy the 'ticktock' stream:

```
$ curl -X PUT http://localhost:9393/streams/ticktock
```

If successful you should see output similar to the following in the `AdminApplication` console:

```
2015-08-01 23:59:58.244  INFO 32845 --- [hannel-adapter1] sink.LogSink     : Received: 2015-08-01 23:59:58
2015-08-02 00:00:03.245  INFO 32845 --- [hannel-adapter1] sink.LogSink     : Received: 2015-08-02 00:00:03
2015-08-02 00:00:08.247  INFO 32845 --- [hannel-adapter1] sink.LogSink     : Received: 2015-08-02 00:00:08
```
