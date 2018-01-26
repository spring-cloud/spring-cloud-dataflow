package org.springframework.cloud.dataflow.rest.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;

/**
 * Configuration properties used in {@link DataFlowClientAutoConfiguration}
 *
 * @author Vinicius Carvalho
 */
@ConfigurationProperties(prefix = DataFlowPropertyKeys.PREFIX + "client")
public class DataFlowClientProperties {

	private String serverUri = "http://localhost:9393";

	private Authentication authentication = new Authentication();

	private boolean skipSslValidation = true;

	private boolean enableDsl = false;

	public boolean isEnableDsl() {
		return enableDsl;
	}

	public void setEnableDsl(boolean enableDsl) {
		this.enableDsl = enableDsl;
	}

	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}

	public String getServerUri() {
		return serverUri;
	}

	public void setServerUri(String serverUri) {
		this.serverUri = serverUri;
	}

	public Authentication getAuthentication() {
		return authentication;
	}

	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}

	public static class Authentication {

		private Basic basic = new Basic();

		public Basic getBasic() {
			return basic;
		}

		public void setBasic(Basic basic) {
			this.basic = basic;
		}

		public static class Basic {

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
}
