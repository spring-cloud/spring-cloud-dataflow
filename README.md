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

The [Admin](https://github.com/spring-cloud/spring-cloud-data/tree/master/spring-cloud-data-admin) provides a REST API and UI. It is an executable Spring Boot application that is profile aware, so that the proper implementation of the Module Deployer SPI will be instantiated based on the environment within which the Admin application itself is running.

The [Shell](https://github.com/spring-cloud/spring-cloud-data/tree/master/spring-cloud-data-shell) connects to the Admin's REST API and supports a DSL that simplifies the process of defining a stream and managing its lifecycle.

The instructions below describe the process of running both the Admin and the Shell across different runtime environments.

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
$ java -jar spring-cloud-data-admin/target/spring-cloud-data-admin-1.0.0.BUILD-SNAPSHOT.jar
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

2\. launch the admin, with a mapping for port 9393 and extra memory (the default is 128MB):

```
ltc create admin springcloud/data-admin -p 9393 -m 512
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

## Running on Hadoop YARN

Current YARN configuration is set to use localhost meaning this can only be run against local cluster. Also all commands needs to be run from a project root.

1\. build packages

```
$ mvn clean package
```

2\. start Redis locally via `redis-server`

3\. optionally wipe existing data on `hdfs`

```
$ hdfs dfs -rm -R /app/app
```

4\. start `spring-cloud-data-admin` with `yarn` profile

```
$ java -Dspring.profiles.active=yarn -jar spring-cloud-data-admin/target/spring-cloud-data-admin-1.0.0.BUILD-SNAPSHOT.jar
```

5\. start `spring-cloud-data-shell`

```
$ java -jar spring-cloud-data-shell/target/spring-cloud-data-shell-1.0.0.BUILD-SNAPSHOT.jar

cloud-data:>stream create --name "ticktock" --definition "time --fixedDelay=5|log" --deploy

cloud-data:>stream list
  Stream Name  Stream Definition        Status
  -----------  -----------------------  --------
  ticktock     time --fixedDelay=5|log  deployed

cloud-data:>stream destroy --name "ticktock"
Destroyed stream 'ticktock'
```

YARN application is pushed and started automatically during a stream deployment process. This application instance is not automatically closed which can be done from CLI:

```
$ java -jar spring-cloud-data-yarn/spring-cloud-data-yarn-client/target/spring-cloud-data-yarn-client-1.0.0.BUILD-SNAPSHOT.jar shell
Spring YARN Cli (v2.3.0.M2)
Hit TAB to complete. Type 'help' and hit RETURN for help, and 'exit' to quit.

$ submitted
  APPLICATION ID                  USER          NAME                            QUEUE    TYPE       STARTTIME       FINISHTIME  STATE    FINALSTATUS  ORIGINAL TRACKING URL
  ------------------------------  ------------  ------------------------------  -------  ---------  --------------  ----------  -------  -----------  --------------------------
  application_1439803106751_0088  jvalkealahti  spring-cloud-data-yarn-app_app  default  CLOUDDATA  01/09/15 09:02  N/A         RUNNING  UNDEFINED    http://192.168.122.1:48913

$ shutdown -a application_1439803106751_0088
shutdown requested
```

