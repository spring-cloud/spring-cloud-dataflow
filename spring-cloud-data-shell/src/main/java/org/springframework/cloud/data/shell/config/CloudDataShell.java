package org.springframework.cloud.data.shell.config;

import org.springframework.cloud.data.rest.client.CloudDataOperations;
import org.springframework.stereotype.Component;

/**
 * REST client component that holds all the operations communicating to spring cloud data REST server.
 *
 * @author Ilayaperumal Gopinathan
 */
@Component
public class CloudDataShell {

	private CloudDataOperations CloudDataOperations;

	public CloudDataOperations getCloudDataOperations() {
		return CloudDataOperations;
	}

	public void setCloudDataOperations(CloudDataOperations cloudDataOperations) {
		this.CloudDataOperations = cloudDataOperations;
	}

}
