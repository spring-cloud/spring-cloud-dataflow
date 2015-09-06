# spring-cloud-dataflow

The Spring Cloud Data Flow project provides orchestration for data microservices, including
both [stream](https://github.com/spring-cloud/spring-cloud-stream) and
[task](https://github.com/spring-cloud/spring-cloud-task) processing modules.

## Components

The [Core](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-core)
domain module includes the concept of a **stream** that is a composition of spring-cloud-stream
modules in a linear pipeline from a *source* to a *sink*, optionally including *processor* modules
in between. The domain also includes the concept of a **task**, which may be any process that does
not run indefinitely, including [Spring Batch](https://github.com/spring-projects/spring-batch) jobs.

The [Module Registry](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-registry)
maintains the set of available modules, and their mappings to Maven coordinates.

The [Module Deployer SPI](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-spi)
provides the abstraction layer for deploying the modules of a given stream across a variety of runtime environments, including:
* [Local](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-local)
* [Lattice](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-lattice)
* [Cloud Foundry](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-cloudfoundry)
* [Yarn](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-module-deployers/spring-cloud-dataflow-module-deployer-yarn)

The [Admin](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-admin) provides a REST API and UI. It is an executable Spring Boot application that is profile aware, so that the proper implementation of the Module Deployer SPI will be instantiated based on the environment within which the Admin application itself is running.

The [Shell](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-shell) connects to the Admin's REST API and supports a DSL that simplifies the process of defining a stream and managing its lifecycle.

The instructions below describe the process of running both the Admin and the Shell across different runtime environments.

## Running Singlenode

1\. start Redis locally via `redis-server`

2\. clone this repository and build from the root directory:

```
git clone https://github.com/spring-cloud/spring-cloud-dataflow.git
cd spring-cloud-dataflow
mvn clean package
```

3\. launch the admin:

```
$ java -jar spring-cloud-dataflow-admin/target/spring-cloud-dataflow-admin-1.0.0.BUILD-SNAPSHOT.jar
```

4\. launch the shell:

```
$ java -jar spring-cloud-dataflow-shell/target/spring-cloud-dataflow-shell-1.0.0.BUILD-SNAPSHOT.jar
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
ltc create admin springcloud/dataflow-admin -p 9393 -m 512
```

3\. launching the shell is the same as above, but once running must be
configured to point to the admin that is running on Lattice:

```
server-unknown:>admin config server http://admin.192.168.11.11.xip.io
Successfully targeted http://admin.192.168.11.11.xip.io
dataflow:>
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

4\. start `spring-cloud-dataflow-admin` with `yarn` profile

```
$ java -Dspring.profiles.active=yarn -Ddataflow.yarn.app.appmaster.path=spring-cloud-dataflow-yarn/spring-cloud-dataflow-yarn-appmaster/target -Ddataflow.yarn.app.container.path=spring-cloud-dataflow-yarn/spring-cloud-dataflow-yarn-container/target -jar spring-cloud-dataflow-admin/target/spring-cloud-dataflow-admin-1.0.0.BUILD-SNAPSHOT.jar
```

5\. start `spring-cloud-dataflow-shell`

```
$ java -jar spring-cloud-dataflow-shell/target/spring-cloud-dataflow-shell-1.0.0.BUILD-SNAPSHOT.jar

dataflow:>stream create --name "ticktock" --definition "time --fixedDelay=5|log" --deploy

dataflow:>stream list
  Stream Name  Stream Definition        Status
  -----------  -----------------------  --------
  ticktock     time --fixedDelay=5|log  deployed

dataflow:>stream destroy --name "ticktock"
Destroyed stream 'ticktock'
```

YARN application is pushed and started automatically during a stream deployment process. This application instance is not automatically closed which can be done from CLI:

```
$ java -jar spring-cloud-dataflow-yarn/spring-cloud-dataflow-yarn-client/target/spring-cloud-dataflow-yarn-client-1.0.0.BUILD-SNAPSHOT.jar shell
Spring YARN Cli (v2.3.0.M2)
Hit TAB to complete. Type 'help' and hit RETURN for help, and 'exit' to quit.

$ submitted
  APPLICATION ID                  USER          NAME                            QUEUE    TYPE       STARTTIME       FINISHTIME  STATE    FINALSTATUS  ORIGINAL TRACKING URL
  ------------------------------  ------------  ----------------------------------  -------  --------  --------------  ----------  -------  -----------  --------------------------
  application_1439803106751_0088  jvalkealahti  spring-cloud-dataflow-yarn-app_app  default  DATAFLOW  01/09/15 09:02  N/A         RUNNING  UNDEFINED    http://192.168.122.1:48913

$ shutdown -a application_1439803106751_0088
shutdown requested
```

Properties `dataflow.yarn.app.appmaster.path` and `dataflow.yarn.app.container.path` can be used with both `spring-cloud-dataflow-admin` and `and spring-cloud-dataflow-yarn-client` to define directory for `appmaster` and `container` jars. Values for those default to `.` which then assumes all needed jars are in a same working directory.
