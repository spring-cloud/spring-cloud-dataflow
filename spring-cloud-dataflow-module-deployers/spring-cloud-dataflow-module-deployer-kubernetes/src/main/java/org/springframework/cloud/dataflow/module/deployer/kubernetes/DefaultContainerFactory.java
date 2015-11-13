package org.springframework.cloud.dataflow.module.deployer.kubernetes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.deployer.ModuleArgumentQualifier;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;

/**
 * Create a Kubernetes {@link Container} that will be started as part of a
 * Kubernetes Pod by using the default Spring Cloud Module Launcher
 * approach that pulls the required module from a Maven repository.
 *
 * @author Florian Rosenberg
 */
public class DefaultContainerFactory implements ContainerFactory {

	private static final String HEALTH_ENDPOINT = "/health";

	private static final String CONNECTOR_DEPENDENCY = "org.springframework.cloud:spring-cloud-kubernetes-connector:1.0.0.BUILD-SNAPSHOT";

	@Autowired
	protected KubernetesModuleDeployerProperties properties;

	@Override
	public Container create(ModuleDeploymentRequest request, int port) {
		ContainerBuilder container = new ContainerBuilder();
		
		ModuleDeploymentId id = ModuleDeploymentId
				.fromModuleDefinition(request.getDefinition());

		container.withName(KubernetesUtils.createKubernetesName(id))
				.withImage(deduceImageName(request))
				.withArgs(createCommandArgs(request))
				.addNewPort()
					.withContainerPort(port)
				.endPort()
				.withReadinessProbe(
						createProbe(port, properties.getReadinessProbeTimeout(),
								properties.getReadinessProbeDelay()))
				.withLivenessProbe(
						createProbe(port, properties.getLivenessProbeTimeout(),
								properties.getLivenessProbeDelay()));
		return container.build();
	}

	protected String deduceImageName(ModuleDeploymentRequest request) {
		return properties.getModuleLauncherImage();
	}

	protected String bashEscape(String original) {
		// Adapted from http://ruby-doc.org/stdlib-1.9.3/libdoc/shellwords/rdoc/Shellwords.html#method-c-shellescape
		return original.replaceAll("([^A-Za-z0-9_\\-.,:\\/@\\n])", "\\\\$1").replaceAll("\n", "'\\\\n'");
	}
	/**
	 * Create a readiness probe for the /health endpoint exposed by each module.
	 */
	protected Probe createProbe(Integer externalPort, long timeout, long initialDelay) {
		return new ProbeBuilder()
			.withHttpGet(
				new HTTPGetActionBuilder()
					.withPath(HEALTH_ENDPOINT)
					.withNewPort(externalPort)
					.build()
			)
			.withTimeoutSeconds(timeout)
			.withInitialDelaySeconds(initialDelay)
			.build();
	}

	protected List<String> createCommandArgs(ModuleDeploymentRequest request) {
		Map<String, String> args = new HashMap<>();
		args.put("modules", request.getCoordinates().toString());
		args.putAll(properties.getLauncherProperties());

		Map<String, String> argsToQualify = new HashMap<>();
		argsToQualify.putAll(request.getDefinition().getParameters());
		argsToQualify.putAll(request.getDeploymentProperties());
		String jmxDomainName = String.format("%s.%s", request.getDefinition().getGroup(), request.getDefinition().getLabel());
		argsToQualify.put(ModuleDeployer.JMX_DEFAULT_DOMAIN_KEY, jmxDomainName);

		String includes = argsToQualify.get("includes");
		if (includes != null) {
			includes += "," + CONNECTOR_DEPENDENCY;
		} else {
			includes = CONNECTOR_DEPENDENCY;
		}
		argsToQualify.put("includes", includes);
		argsToQualify.put("spring.profiles.active", "cloud"); // This triggers the use of the kubernetes connector
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, argsToQualify));
		List<String> cmdArgs = new LinkedList<String>();
		for (Map.Entry<String, String> entry : args.entrySet()) {

			cmdArgs.add(String.format("--%s=%s", bashEscape(entry.getKey()),
					bashEscape(entry.getValue())));
		}
		return cmdArgs;
	}

}
