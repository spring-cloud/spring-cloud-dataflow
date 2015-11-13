package org.springframework.cloud.dataflow.module.deployer.kubernetes;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.module.ModuleInstanceStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus.State;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;

/**
 * Represents the status of a module.
 * @author Florian Rosenberg
 */
public class KubernetesModuleInstanceStatus implements ModuleInstanceStatus {

	private final Pod pod;
	private final String moduleId;
	private ContainerStatus containerStatus;

	public KubernetesModuleInstanceStatus(String moduleId, Pod pod) {
		this.moduleId = moduleId;
		this.pod = pod;
		// we assume one container per pod
		if (pod != null && pod.getStatus().getContainerStatuses().size() == 1) {
			this.containerStatus = pod.getStatus().getContainerStatuses().get(0);
		} else {
			this.containerStatus = null;
		}
	}

	@Override
	public String getId() {
		return String.format("%s:%s", moduleId, pod.getMetadata().getName());
	}

	@Override
	public ModuleStatus.State getState() {
		return pod != null && containerStatus != null ? mapState() : ModuleStatus.State.unknown;
	}

	// Maps Kubernetes phases onto Spring Cloud Dataflow states
	private State mapState() {
		
		switch (pod.getStatus().getPhase()) {
			
			case "Pending":
				return ModuleStatus.State.deploying;
				
			// We only report a module as running if the container is also ready to service requests.
			// We also implement the Readiness check as part of the container to ensure ready means
			// that the module is up and running and not only that the JVM has been created and the
			// Spring module is still starting up
			case "Running":
				// we assume we only have one container
				if (containerStatus.getReady())
					return ModuleStatus.State.deployed;
				else
					return ModuleStatus.State.deploying;

			case "Succeeded":
				return ModuleStatus.State.complete;

			case "Failed":
				return ModuleStatus.State.failed;

			case "Unknown":
				return ModuleStatus.State.unknown;

			default: 
				return ModuleStatus.State.unknown;			
		}
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> result = new HashMap<>();

		result.put("pod_starttime", pod.getStatus().getStartTime());
		result.put("pod_ip", pod.getStatus().getPodIP());
		result.put("host_ip", pod.getStatus().getHostIP());
		result.put("container_restart_count", ""+ containerStatus.getRestartCount());

		// TODO add more useful stuff
		return result;
	}
}
