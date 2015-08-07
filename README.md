# spring-cloud-data

The spring-cloud-data project provides orchestration for data microservices, including
[spring-cloud-stream](https://github.com/spring-cloud/spring-cloud-stream) modules.
The spring-cloud-data domain model includes the concept of a **stream** that is a composition
of spring-cloud-stream modules in a linear pipeline from a *source* to a *sink*.

## Components

The [Module Registry](https://github.com/spring-cloud/spring-cloud-data/tree/master/spring-cloud-data-module-registry)
maintains the set of available modules, and their mappings to Maven coordinates.

The [Module Deployer SPI](https://github.com/spring-cloud/spring-cloud-data/tree/master/spring-cloud-data-module-deployers/spring-cloud-data-module-deployer-spi)
provides the abstraction layer for deploying the modules of a given stream across a variety of runtime environments, including:
* [Local](https://github.com/spring-cloud/spring-cloud-data/tree/master/spring-cloud-data-module-deployers/spring-cloud-data-module-deployer-local)
* [Lattice](https://github.com/spring-cloud/spring-cloud-data/tree/master/spring-cloud-data-module-deployers/spring-cloud-data-module-deployer-lattice)
* [Cloud Foundry](https://github.com/spring-cloud/spring-cloud-data/tree/master/spring-cloud-data-module-deployers/spring-cloud-data-module-deployer-cloudfoundry)

The [REST API](https://github.com/spring-cloud/spring-cloud-data/tree/master/spring-cloud-data-rest) is provided by an executable Spring Boot application
that is profile aware, so that the proper implementation of the Module Deployer SPI will be instantiated based on the environment within which the Admin
application itself is running.

The [Shell](https://github.com/spring-cloud/spring-cloud-data/tree/master/spring-cloud-data-shell) connects to that REST API and supports a DSL that simplifies the process of defining a stream and managing its lifecycle.

The instructions below describe the process of running both the Shell and the Admin across different runtime environments.

## Running Singlenode

1\. start Redis locally via `redis-server`

2\. clone this repository and build from the root directory:

```
git clone https://github.com/spring-cloud/spring-cloud-data.git
cd spring-cloud-data
mvn clean package
```

3\. launch the admin:

```
$ java -jar spring-cloud-data-rest/target/spring-cloud-data-rest-1.0.0.BUILD-SNAPSHOT.jar
```

4\. launch the shell:

```
$ java -jar spring-cloud-data-shell/target/spring-cloud-data-shell-1.0.0.BUILD-SNAPSHOT.jar
```

thus far, only the following commands are supported in the shell when running singlenode:
* `stream list`
* `stream create`
* `stream deploy`

## Running on Lattice

1\. start Redis on Lattice (running as root):

```
ltc create redis redis -r
```

2\. launch the admin, with a mapping for port 9393:

```
ltc create admin springcloud/data-admin -p 9393
```

3\. launching the shell is the same as above, but once running must be
configured to point to the admin that is running on Lattice:

```
server-unknown:>admin config server http://admin.192.168.11.11.xip.io
Successfully targeted http://admin.192.168.11.11.xip.io
cloud-data:>
```

all stream commands are supported in the shell when running on Lattice:
* `stream list`
* `stream create`
* `stream deploy`
* `stream undeploy`
* `stream all undeploy`
* `stream destroy`
* `stream all destroy`

## Running on Cloud Foundry

*work in progress, stay tuned!*
