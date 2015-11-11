# Spring Cloud Data Flow Admin Starter

The `spring-cloud-dataflow-admin-starter` subproject of `spring-cloud-dataflow` provides the REST API and UI for implementations of the Admin SPI.

Currently the REST API includes a `StreamController` and `TaskController` that interact with in-memory implementations of the `StreamDefinitionRepository` and `TaskDefinitionRepository`, respectively. This project also provides the `ArtifactRegistry` which is populated by default with the out-of-the-box source, processor, and sink modules from the `spring-cloud-stream-modules` project.

For a simple getting-started experience, see the `spring-cloud-dataflow-admin-local` (sibling) project. The build for that project produces a boot-executable `AdminApplication` that relies on a `LocalModuleDeployer` implementation for launching modules within the same process as the `AdminApplication` itself.
