package org.springframework.cloud.dataflow.module.deployer.kubernetes;

import static org.springframework.cloud.dataflow.module.deployer.kubernetes.KubernetesUtils.createKubernetesName;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;


/**
 * Implements a deployer for the Google Kubernetes project. 
 * 
 * @author Florian Rosenberg
 */
public class KubernetesModuleDeployer implements ModuleDeployer {

	protected static final String SCSM_GROUP_KEY = "scsm-group";
	protected static final String SCSM_LABEL_KEY = "scsm-label";
	private static final String SCSM_EXTENSION = "scsm-extension";
	private static final String SCSM_VERSION = "scsm-version";
	private static final String SCSM_GROUP_ID = "scsm-groupId";
	private static final String SCSM_ARTIFACT_ID = "scsm-artifactId";
	private static final String SPRING_MARKER_VALUE = "scsm-module";
	private static final String SCSM_CLASSIFIER = "scsm-classifier";
	private static final String SPRING_MARKER_KEY = "role";
	private static final String PORT_KEY = "port";
	private static final String SERVER_PORT_KEY = "server.port";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private KubernetesClient kubernetesClient;

	@Autowired
	private ContainerFactory containerFactory;
	
	private KubernetesModuleDeployerProperties properties;

	public KubernetesModuleDeployer(
			KubernetesModuleDeployerProperties properties) {
		this.properties = properties;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.cloud.dataflow.module.deployer.ModuleDeployer#deploy(org.springframework.cloud.dataflow.core.ModuleDeploymentRequest)
	 */
	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {
		ModuleDeploymentId id = ModuleDeploymentId.fromModuleDefinition(request.getDefinition());

		logger.debug("Deploying module: {}", createKubernetesName(id));

		int externalPort = 8080;
		// we also create a service in case have an '--port' or '--server.port' argument (source or sink)
		Map<String, String> parameters = request.getDefinition().getParameters();
		if (parameters.containsKey(PORT_KEY)) {
			externalPort = Integer.valueOf(parameters.get(PORT_KEY));
			createService(id, request, externalPort);
		} else if (parameters.containsKey(SERVER_PORT_KEY)) {
			externalPort = Integer.valueOf(parameters.get(SERVER_PORT_KEY));		
			createService(id, request, externalPort);
		}		
		createReplicationController(id, request, externalPort);				
		return id;	
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.cloud.dataflow.module.deployer.ModuleDeployer#undeploy(org.springframework.cloud.dataflow.core.ModuleDeploymentId)
	 */
	@Override
	public void undeploy(ModuleDeploymentId id) {
		String name = createKubernetesName(id);
		logger.debug("Undeploying module: {}", name);

		Map<String, String> idMap = createIdMap(id);
		try {
			kubernetesClient.services().withLabels(idMap).delete();
			kubernetesClient.replicationControllers().withLabels(idMap).delete();
			kubernetesClient.pods().withLabels(idMap).delete();
		} catch (KubernetesClientException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.cloud.dataflow.module.deployer.ModuleDeployer#status(org.springframework.cloud.dataflow.core.ModuleDeploymentId)
	 */
	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		String name = createKubernetesName(id);
		logger.debug("Querying module status: {}", name);

		try {
			// The only really interesting status is coming from the containers and pods.
			// The service and the RC don't have "realtime" status info.
			PodList list = kubernetesClient.pods().withLabels(createIdMap(id)).list();
			return buildModuleStatus(id, list);
		}
		catch (KubernetesClientException e) {
			logger.warn(e.getMessage(), e);
			return buildModuleStatus(id, null);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.cloud.dataflow.module.deployer.ModuleDeployer#status()
	 */
	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		Map<ModuleDeploymentId, ModuleStatus> result = new HashMap<>();
		
		ReplicationControllerList list = 
				kubernetesClient.replicationControllers()
				.withLabel(SPRING_MARKER_KEY, SPRING_MARKER_VALUE).list();
		
		for (ReplicationController rc : list.getItems()) {
			Map<String, String> labels = rc.getMetadata().getLabels();			
			String group = labels.get(SCSM_GROUP_KEY);
			String label = labels.get(SCSM_LABEL_KEY);			
			ModuleDeploymentId id = new ModuleDeploymentId(group, label);
			
			PodList pods = kubernetesClient.pods().withLabels(labels).list();
			result.put(id, buildModuleStatus(id, pods));	
		}		
		return result;		
	}

	
	private ReplicationController createReplicationController(
			ModuleDeploymentId id, ModuleDeploymentRequest request,
			int externalPort) {
		ReplicationController rc = new ReplicationControllerBuilder()
			.withNewMetadata()
				.withName(createKubernetesName(id)) // does not allow . in the name
				.withLabels(createIdMap(id))
					.addToLabels(SPRING_MARKER_KEY, SPRING_MARKER_VALUE)
					.addToLabels(SCSM_ARTIFACT_ID, request.getCoordinates().getArtifactId())
					.addToLabels(SCSM_GROUP_ID, request.getCoordinates().getGroupId())
					.addToLabels(SCSM_VERSION, request.getCoordinates().getVersion())
					.addToLabels(SCSM_EXTENSION, request.getCoordinates().getExtension())
					.addToLabels(SCSM_CLASSIFIER, request.getCoordinates().getClassifier())
			.endMetadata()
			.withNewSpec()
				.withReplicas(request.getCount())
				.withSelector(createIdMap(id))
				.withNewTemplate()
					.withNewMetadata()
						.withLabels(createIdMap(id))
						.addToLabels(SPRING_MARKER_KEY, SPRING_MARKER_VALUE)
					.endMetadata()
					.withSpec(createPodSpec(request, externalPort))
				.endTemplate()
			.endSpec()
			.build();

		return kubernetesClient.replicationControllers().create(rc);
	}
	
	private PodSpec createPodSpec(ModuleDeploymentRequest request, int port) {
		PodSpecBuilder podSpec = new PodSpecBuilder();

		// Add image secrets if set
		if (properties.getImagePullSecret() != null) {
			podSpec.addNewImagePullSecret(properties.getImagePullSecret());
		}

		Container container = containerFactory.create(request, port);

		// add memory and cpu resource limits
		ResourceRequirements req = new ResourceRequirements();
		req.setLimits(deduceResourceLimits(request));
		container.setResources(req);

		podSpec.addToContainers(container);
		return podSpec.build();
	}

	private void createService(ModuleDeploymentId id, ModuleDeploymentRequest request, int externalPort) {
		kubernetesClient.services().inNamespace(kubernetesClient.getNamespace()).createNew()
			.withNewMetadata()
				.withName(KubernetesUtils.createKubernetesName(id)) // does not allow . in the name
				.withLabels(createIdMap(id))
				.addToLabels(SPRING_MARKER_KEY, SPRING_MARKER_VALUE)
			.endMetadata()
			.withNewSpec()
				.withSelector(createIdMap(id))
				.addNewPort()
					.withPort(externalPort)
				.endPort()
			.endSpec()
			.done();
	}

	/**
	 * Creates a map of labels for a given ID. This will allow Kubernetes services
	 * to "select" the right ReplicationControllers.
	 */
	private Map<String, String> createIdMap(ModuleDeploymentId id) {
		Map<String, String> map = new HashMap<>();
		map.put(SCSM_GROUP_KEY, id.getGroup());
		map.put(SCSM_LABEL_KEY, id.getLabel());
		return map;
	}

	private ModuleStatus buildModuleStatus(ModuleDeploymentId id, PodList list) {
		ModuleStatus.Builder statusBuilder = ModuleStatus.of(id);
		String moduleId = id.toString();

		if (list == null) {
			statusBuilder.with(new KubernetesModuleInstanceStatus(moduleId, null));
		} else {

			for (Pod pod : list.getItems()) {
				statusBuilder.with(new KubernetesModuleInstanceStatus(moduleId, pod));
			}
		}
		return statusBuilder.build();
	}

	private Map<String, Quantity> deduceResourceLimits(ModuleDeploymentRequest request) {
		String memOverride = request.getDeploymentProperties().get("kubernetes.memory");
		if (memOverride == null)
			memOverride = properties.getMemory();

		String cpuOverride = request.getDeploymentProperties().get("kubernetes.cpu");
		if (cpuOverride == null)
			cpuOverride = properties.getCpu();


		Map<String,Quantity> limits = new HashMap<String,Quantity>();
		limits.put("memory", new Quantity(memOverride));
		limits.put("cpu", new Quantity(cpuOverride));
		return limits;
	}

}
