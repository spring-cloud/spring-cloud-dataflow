<p align="center">
  <a href="https://dataflow.spring.io/">
    <img alt="Spring Data Flow Dashboard" title="Spring Data Flow" src="https://i.imgur.com/hpeKaRk.png" width="450" />
  </a>
</p>

<p align="center">
  <a href="https://dataflow.spring.io/getting-started/">
    <img src="https://spring.io/badges/spring-cloud-dataflow/ga.svg"
         alt="Latest Release Version" />
  </a>
  <a href="https://dataflow.spring.io/getting-started/">
    <img src="https://spring.io/badges/spring-cloud-dataflow/snapshot.svg"
         alt="Latest Snapshot Version" />
  </a>
  <br>
  <a href="https://build.spring.io/browse/SCD-BMASTER">
    <img src="https://build.spring.io/plugins/servlet/wittified/build-status/SCD-BMASTER"
         alt="Build Status" />
  </a>
</p>


*Spring Cloud Data Flow* is a toolkit for building data integration and real-time data processing pipelines. 

Pipelines consist of Spring Boot apps, built using the [Spring Cloud Stream](https://github.com/spring-cloud/spring-cloud-stream)
or [Spring Cloud Task](https://github.com/spring-cloud/spring-cloud-task) microservice frameworks. 

This makes Spring Cloud Data Flow suitable for a range of data processing use cases, from import/export to 
event streaming and predictive analytics.

----

## Components

The [Core](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-core)
domain module includes the concept of a *stream* that is a composition of spring-cloud-stream
modules in a linear pipeline from a *source* to a *sink*, optionally including *processor* application(s)
in between. The domain also includes the concept of a *task*, which may be any process that does
not run indefinitely, including [Spring Batch](https://github.com/spring-projects/spring-batch) jobs.

The [App Registry](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-registry)
maintains the set of available apps, and their mappings to URIs.
For example, if relying on Maven coordinates, an app's URI would be of the format:
`maven://<groupId>:<artifactId>:<version>`

The Data Flow Server is a Spring Boot application that provides a common REST API and UI.
As of version 2.0 a single Data Flow Server supports deploying tasks to Local, Cloud Foundry, and Kubernetes.
Also as of version 2.0, the Skipper Server is required for deploying streams to Local, Cloud Foundry and Kubernetes.
The github locations for the Data Flow Server is:

* https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-server

There are also community maintained *Spring Cloud Data Flow* implementations that are currently based on the 1.7.x series of Data Flow but will eventually get updated to the 2.0 baseline.

 * [HashiCorp Nomad](https://github.com/donovanmuller/spring-cloud-dataflow-server-nomad)
 * [OpenShift](https://github.com/donovanmuller/spring-cloud-dataflow-server-openshift)
 * [Apache Mesos](https://github.com/trustedchoice/spring-cloud-dataflow-server-mesos)

The [Apache YARN](https://github.com/spring-cloud/spring-cloud-dataflow-server-yarn) implementation has reached end-of-line status. Let us know at [Gitter](https://gitter.im/spring-cloud/spring-cloud-dataflow) if youare interested in forking the project to continue developing and maintaining it.

The deployer SPI mentioned above is defined within the [Spring Cloud Deployer](https://github.com/spring-cloud/spring-cloud-deployer)
project. That provides an abstraction layer for deploying the apps of a given stream or task and managing their lifecycle.
The github locations for the corresponding Spring Cloud Deployer SPI implementations are:

* [Local](https://github.com/spring-cloud/spring-cloud-deployer-local)
* [Cloud Foundry](https://github.com/spring-cloud/spring-cloud-deployer-cloudfoundry)
* [Kubernetes](https://github.com/spring-cloud/spring-cloud-deployer-kubernetes)


The [Shell](https://github.com/spring-cloud/spring-cloud-dataflow/tree/master/spring-cloud-dataflow-shell)
connects to the Data Flow Server's REST API and supports a DSL that simplifies the process of
defining a stream or task and managing its lifecycle.

Instructions for running the Data Flow Server for each runtime environment can be found in their respective github repositories.

----

## Building

Clone the repo and type 

    $ ./mvnw clean install 

For more information on building, see this [link](https://github.com/spring-cloud/spring-cloud-dataflow/blob/master/spring-cloud-dataflow-docs/src/main/asciidoc/appendix-building.adoc).

### Building on Windows

When using Git on Windows to check out the project, it is important to handle line-endings correctly during checkouts. By default Git will change the line-endings during checkout to `CRLF`. This is, however, not desired for _Spring Cloud Data Flow_ as this may lead to test failures under Windows.

Therefore, please ensure that you set Git property `core.autocrlf` to `false`, e.g. using: `$ git config core.autocrlf false`. Fore more information please refer to the [Git documentation, Formatting and Whitespace](https://git-scm.com/book/en/v2/Customizing-Git-Git-Configuration).

----

## Contributing

We welcome contributions! Follow this [link](https://github.com/spring-cloud/spring-cloud-dataflow/blob/master/spring-cloud-dataflow-docs/src/main/asciidoc/appendix-contributing.adoc) for more information on how to contribute.

----

## Reporting Issues

When reporting problems, it'd be helpful if the bug report includes the details listed on this [wiki-article](https://github.com/spring-cloud/spring-cloud-dataflow/wiki/Reporting-Issues). 

----

## Code formatting guidelines

* The directory ./src/eclipse has two files for use with code formatting, `eclipse-code-formatter.xml` for the majority of the code formatting rules and `eclipse.importorder` to order the import statements.

* In eclipse you import these files by navigating `Windows -> Preferences` and then the menu items `Preferences > Java > Code Style > Formatter` and `Preferences > Java > Code Style > Organize Imports` respectfully.

* In `IntelliJ`, install the plugin `Eclipse Code Formatter`.  You can find it by searching the "Browse Repositories" under the plugin option within `IntelliJ` (Once installed you will need to reboot Intellij for it to take effect).
Then navigate to `Intellij IDEA > Preferences` and select the Eclipse Code Formatter.  Select the `eclipse-code-formatter.xml` file for the field `Eclipse Java Formatter config file` and the file `eclipse.importorder` for the field `Import order`.
Enable the `Eclipse code formatter` by clicking `Use the Eclipse code formatter` then click the *OK* button.
** NOTE: If you configure the `Eclipse Code Formatter` from `File > Other Settings > Default Settings` it will set this policy across all of your Intellij projects.
