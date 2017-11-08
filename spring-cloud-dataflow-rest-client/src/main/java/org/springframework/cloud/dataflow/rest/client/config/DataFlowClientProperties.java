package org.springframework.cloud.dataflow.rest.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;

/**
 * @author Vinicius Carvalho
 */
@ConfigurationProperties(prefix = DataFlowPropertyKeys.PREFIX + ".client")
public class DataFlowClientProperties {

	private String target = "http://localhost:9393";

	private Security security = new Security();

	private boolean skipSslValidation = true;

	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public Security getSecurity() {
		return security;
	}

	public void setSecurity(Security security) {
		this.security = security;
	}

	public static class Security {

		private String username;
		private String password;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
}
