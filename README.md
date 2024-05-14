<p align="center">
  <a href="https://dataflow.spring.io/">
    <img alt="Spring Data Flow Dashboard" title="Spring Data Flow" src="https://i.imgur.com/hpeKaRk.png" width="450" />
  </a>
</p>

[![Build Status - CI](https://github.com/spring-cloud/spring-cloud-dataflow/actions/workflows/ci.yml/badge.svg)](https://github.com/spring-cloud/spring-cloud-dataflow/actions/workflows/ci.yml)


*Spring Cloud Data Flow* is a microservices-based toolkit for building streaming and batch data processing pipelines in
Cloud Foundry and Kubernetes.

Data processing pipelines consist of Spring Boot apps, built using the [Spring Cloud Stream](https://github.com/spring-cloud/spring-cloud-stream)
or [Spring Cloud Task](https://github.com/spring-cloud/spring-cloud-task) microservice frameworks. 

This makes Spring Cloud Data Flow ideal for a range of data processing use cases, from import/export to event streaming
and predictive analytics.

----

## Components

**Architecture**: The Spring Cloud Data Flow Server is a Spring Boot application that provides RESTful API and REST clients
(Shell, Dashboard, Java DSL).
A single Spring Cloud Data Flow installation can support orchestrating the deployment of streams and tasks to Local,
Cloud Foundry, and Kubernetes.

Familiarize yourself with the Spring Cloud Data Flow [architecture](https://dataflow.spring.io/docs/concepts/architecture/)
and [feature capabilities](https://dataflow.spring.io/features/).

**Deployer SPI**: A Service Provider Interface (SPI) is defined in the [Spring Cloud Deployer](https://github.com/spring-cloud/spring-cloud-deployer)
project. The Deployer SPI provides an abstraction layer for deploying the apps for a given streaming or batch data pipeline
and managing the application lifecycle.

Spring Cloud Deployer Implementations:

* [Local](https://github.com/spring-cloud/spring-cloud-deployer-local)
* [Cloud Foundry](https://github.com/spring-cloud/spring-cloud-deployer-cloudfoundry)
* [Kubernetes](https://github.com/spring-cloud/spring-cloud-deployer-kubernetes)

**Domain Model**: The Spring Cloud Data Flow [domain module](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-core)
includes the concept of a *stream* that is a composition of Spring Cloud Stream applications in a linear data pipeline
from a *source* to a *sink*, optionally including *processor* application(s) in between. The domain also includes the
concept of a *task*, which may be any process that does not run indefinitely, including [Spring Batch](https://github.com/spring-projects/spring-batch)
jobs.

**Application Registry**: The [App Registry](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-registry)
maintains the metadata of the catalog of reusable applications.
For example, if relying on Maven coordinates, an application URI would be of the format:
`maven://<groupId>:<artifactId>:<version>`.

**Shell/CLI**: The [Shell](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-shell)
connects to the Spring Cloud Data Flow Server's REST API and supports a DSL that simplifies the process of defining a
stream or task and managing its lifecycle.

----

## Building

Clone the repo and type 

    $ ./mvnw -s .settings.xml clean install 

Looking for more information? Follow this [link](https://github.com/spring-cloud/spring-cloud-dataflow/blob/master/spring-cloud-dataflow-docs/src/main/asciidoc/appendix-building.adoc).

### Building on Windows

When using Git on Windows to check out the project, it is important to handle line-endings correctly during checkouts.
By default Git will change the line-endings during checkout to `CRLF`. This is, however, not desired for _Spring Cloud Data Flow_
as this may lead to test failures under Windows.

Therefore, please ensure that you set Git property `core.autocrlf` to `false`, e.g. using: `$ git config core.autocrlf false`.
For more information please refer to the [Git documentation, Formatting and Whitespace](https://git-scm.com/book/en/v2/Customizing-Git-Git-Configuration).

----

## Running Locally w/ Oracle 
By default, the Dataflow server jar does not include the Oracle database driver dependency.
If you want to use Oracle for development/testing when running locally, you can specify the `local-dev-oracle` Maven profile when building.
The following command will include the Oracle driver dependency in the jar:
```
$ ./mvnw -s .settings.xml clean package -Plocal-dev-oracle
```
You can follow the steps in the [Oracle on Mac ARM64](https://github.com/spring-cloud/spring-cloud-dataflow/wiki/Oracle-on-Mac-ARM64#run-container-in-docker) Wiki to run Oracle XE locally in Docker with Dataflow pointing at it.

> **NOTE:** If you are not running Mac ARM64 just skip the steps related to Homebrew and Colima 

----

## Running Locally w/ Microsoft SQL Server
By default, the Dataflow server jar does not include the MSSQL database driver dependency.
If you want to use MSSQL for development/testing when running locally, you can specify the `local-dev-mssql` Maven profile when building.
The following command will include the MSSQL driver dependency in the jar:
```
$ ./mvnw -s .settings.xml clean package -Plocal-dev-mssql
```
You can follow the steps in the [MSSQL on Mac ARM64](https://github.com/spring-cloud/spring-cloud-dataflow/wiki/MSSQL-on-Mac-ARM64#running-dataflow-locally-against-mssql) Wiki to run MSSQL locally in Docker with Dataflow pointing at it.

> **NOTE:** If you are not running Mac ARM64 just skip the steps related to Homebrew and Colima

----

## Running Locally w/ IBM DB2
By default, the Dataflow server jar does not include the DB2 database driver dependency.
If you want to use DB2 for development/testing when running locally, you can specify the `local-dev-db2` Maven profile when building.
The following command will include the DB2 driver dependency in the jar:
```
$ ./mvnw -s .settings.xml clean package -Plocal-dev-db2
```
You can follow the steps in the [DB2 on Mac ARM64](https://github.com/spring-cloud/spring-cloud-dataflow/wiki/DB2-on-Mac-ARM64#running-dataflow-locally-against-db2) Wiki to run DB2 locally in Docker with Dataflow pointing at it.

> **NOTE:** If you are not running Mac ARM64 just skip the steps related to Homebrew and Colima

----

## Contributing

We welcome contributions! See the [CONTRIBUTING](./CONTRIBUTING.adoc) guide for details.

----

## Code formatting guidelines

* The directory ./src/eclipse has two files for use with code formatting, `eclipse-code-formatter.xml` for the majority of the code formatting rules and `eclipse.importorder` to order the import statements.

* In eclipse you import these files by navigating `Windows -> Preferences` and then the menu items `Preferences > Java > Code Style > Formatter` and `Preferences > Java > Code Style > Organize Imports` respectfully.

* In `IntelliJ`, install the plugin `Eclipse Code Formatter`.  You can find it by searching the "Browse Repositories" under the plugin option within `IntelliJ` (Once installed you will need to reboot Intellij for it to take effect).
Then navigate to `Intellij IDEA > Preferences` and select the Eclipse Code Formatter.  Select the `eclipse-code-formatter.xml` file for the field `Eclipse Java Formatter config file` and the file `eclipse.importorder` for the field `Import order`.
Enable the `Eclipse code formatter` by clicking `Use the Eclipse code formatter` then click the *OK* button.
** NOTE: If you configure the `Eclipse Code Formatter` from `File > Other Settings > Default Settings` it will set this policy across all of your Intellij projects.

## License

Spring Cloud Data Flow is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).
