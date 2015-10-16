package org.springframework.cloud.dataflow.module.deployer.kubernetes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Spring Bean configuration for the {@link KubernetesModuleDeployer}.
 * @author Florian Rosenberg
 */
@Configuration
@EnableConfigurationProperties(KubernetesModuleDeployerProperties.class)
public class KubernetesModuleDeployerConfiguration {
	
	@Autowired
	private KubernetesModuleDeployerProperties properties;
	
	@Bean
	public ModuleDeployer processModuleDeployer() {
		return new KubernetesModuleDeployer(properties);
	}

	@Bean
	public ModuleDeployer taskModuleDeployer() {
		return processModuleDeployer();
	}

	@Bean
	public KubernetesClient kubernetesClient() {
		return new DefaultKubernetesClient();
	}

	@Bean
	public ContainerFactory containerFactory() {
		return new DefaultContainerFactory();
	}
}