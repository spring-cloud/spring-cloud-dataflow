package org.springframework.cloud.dataflow.rest.client.dsl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vinicius Carvalho
 *
 * Base class for different Deployment properties builders @see {@link SkipperDeploymentPropertiesBuilder} @see {@link DeploymentPropertiesBuilder}
 */
public abstract class AbstractPropertiesBuilder{


	protected Map<String, String> deploymentProperties = new HashMap<>();

	protected final String DEPLOYER_PREFIX = "deployer.%s.%s";

	public Map<String, String> build() {
		return Collections.unmodifiableMap(this.deploymentProperties);
	}

}
