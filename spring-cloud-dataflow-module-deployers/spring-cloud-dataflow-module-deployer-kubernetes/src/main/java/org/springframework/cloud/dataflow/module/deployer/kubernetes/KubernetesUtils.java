package org.springframework.cloud.dataflow.module.deployer.kubernetes;

import org.springframework.cloud.dataflow.core.ModuleDeploymentId;

public final class KubernetesUtils {

	// does not allow . in the name
	public static String createKubernetesName(ModuleDeploymentId id) {
		return id.toString().replace('.', '-');
	}
}
