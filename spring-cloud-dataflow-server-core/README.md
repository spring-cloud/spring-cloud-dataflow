# Spring Cloud Data Flow Server Core

The `spring-cloud-dataflow-server-core` subproject of `spring-cloud-dataflow` provides the REST API and UI to be combined with implementations of the Deployer SPI in order to create a Data Flow Server for
a particular deployment environment.

Currently the REST API includes a `StreamDefinitionController` and `TaskDefinitionController` that interact with in-memory implementations of the `StreamDefinitionRepository` and `TaskDefinitionRepository`, respectively. This project also provides the `ArtifactRegistry` which is populated by default with the out-of-the-box source, processor, and sink modules from the `spring-cloud-stream-modules` project.

For a simple getting-started experience, see the `spring-cloud-dataflow-server-local` (sibling) project. The build for that project produces a boot-executable `LocalDataFlowServer` that relies on an implementation of the Deployer SPI that launches each stream app as a new JVM process on the same host as the `LocalDataFlowServer` itself.
