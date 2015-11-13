package org.springframework.cloud.dataflow.module.deployer.kubernetes;

import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;

import io.fabric8.kubernetes.api.model.Container;

/**
 * Defines how a Kubernetes {@link Container} is created.
 *
 * @author Florian Rosenberg
 */
public interface ContainerFactory {

	Container create(ModuleDeploymentRequest request, int externalPort);

}