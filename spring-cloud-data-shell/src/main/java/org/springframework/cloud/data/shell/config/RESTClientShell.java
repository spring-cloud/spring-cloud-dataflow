package org.springframework.cloud.data.shell.config;

import org.springframework.cloud.data.rest.client.RESTClientOperations;
import org.springframework.stereotype.Component;

/**
 * REST client component that holds all the operations communicating to spring cloud data REST server.
 *
 * @author Ilayaperumal Gopinathan
 */
@Component
public class RESTClientShell {

	private RESTClientOperations RESTClientOperations;

	public RESTClientOperations getRESTClientOperations() {
		return RESTClientOperations;
	}

	public void setRESTClientOperations(RESTClientOperations RESTClientOperations) {
		this.RESTClientOperations = RESTClientOperations;
	}

}
