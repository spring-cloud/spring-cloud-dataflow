package org.springframework.cloud.dataflow.module.deployer.kubernetes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.deployer.ModuleArgumentQualifier;

import io.fabric8.kubernetes.api.model.*;

/**
 * Create a Kubernetes {@link Container} that will be started as part of a
 * Kubernetes Pod by using the default Spring Cloud Module Launcher
 * approach that pulls the required module from a Maven repository.
 *
 * @author Florian Rosenberg
 */
public class DefaultContainerFactory implements ContainerFactory {

	private static final String JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";

	private static final String HEALTH_ENDPOINT = "/health";

	private static final String SPRING_REDIS_HOST = "SPRING_REDIS_HOST";
	private static final String SPRING_REDIS_SENTINEL_NODES = "SPRING_REDIS_SENTINEL_NODES";
	private static final String SPRING_REDIS_SENTINEL_MASTER = "SPRING_REDIS_SENTINEL_MASTER";

	@Autowired
	protected KubernetesModuleDeployerProperties properties;

	@Override
	public Container create(ModuleDeploymentRequest request, int port) {
		ContainerBuilder container = new ContainerBuilder();
		
		ModuleDeploymentId id = ModuleDeploymentId
				.fromModuleDefinition(request.getDefinition());

		container.withName(KubernetesUtils.createKubernetesName(id))
				.withImage(deduceImageName(request))
				.withEnv(createModuleLauncherEnvArgs(request))
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
		HashMap<String, String> args = new HashMap<>();
		args.put("modules", request.getCoordinates().toString());
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, request.getDefinition().getParameters()));
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, request.getDeploymentProperties()));

		List<String> cmdArgs = new LinkedList<String>();
		for (Map.Entry<String, String> entry : args.entrySet()) {

			cmdArgs.add(String.format("--%s=%s", bashEscape(entry.getKey()),
					bashEscape(entry.getValue())));
		}
		return cmdArgs;
	}

	/**
	 * TODO change this to a cloud connector approach
	 */
	protected List<EnvVar> createModuleLauncherEnvArgs(ModuleDeploymentRequest request) {
		List<EnvVar> envVars = new LinkedList<EnvVar>();

		// Pass on the same REDIS host configuration that the Spring Admin uses to each Kubernetes
		// RC/POD that it creates. This may be a limitation in case we want different redis per
		// customer. We could move this to module deployment properties if needed.

		// Standard Redis config
		String redisHost = System.getenv(SPRING_REDIS_HOST);
		if (redisHost != null) {
			envVars.add(new EnvVarBuilder()
					.withName(SPRING_REDIS_HOST)
					.withValue(redisHost)
					.build());
		}

		// Redis sentinel config
		String redisSentinelMaster = System.getenv(SPRING_REDIS_SENTINEL_MASTER);
		String redisSentinelHost = System.getenv(SPRING_REDIS_SENTINEL_NODES);
		if (redisSentinelMaster != null) {
			envVars.add(new EnvVarBuilder()
					.withName(SPRING_REDIS_SENTINEL_MASTER)
					.withValue(redisSentinelMaster)
					.build());
		}
		if (redisSentinelHost != null) {
			envVars.add(new EnvVarBuilder()
					.withName(SPRING_REDIS_SENTINEL_NODES)
					.withValue(redisSentinelHost)
					.build());
		}

		// add java opts for the module java process
		String javaOpts = request.getDeploymentProperties().get(JAVA_TOOL_OPTIONS.toLowerCase());
		if (javaOpts != null) {
			envVars.add(new EnvVarBuilder()
					.withName(JAVA_TOOL_OPTIONS)
					.withValue(javaOpts)
					.build());
		}

		return envVars;
	}

}
