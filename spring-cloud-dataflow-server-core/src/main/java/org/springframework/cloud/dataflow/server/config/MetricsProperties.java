/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;

/**
 * Configuration properties for namespace 'spring.cloud.dataflow.metrics'.
 *
 * @author Janne Valkealahti
 */
@ConfigurationProperties(prefix = MetricsProperties.PREFIX)
public class MetricsProperties {

	public static final String PREFIX = DataFlowPropertyKeys.PREFIX + "metrics";

	private Collector collector = new Collector();

	public Collector getCollector() {
		return collector;
	}

	public void setCollector(Collector collector) {
		this.collector = collector;
	}

	public static class Collector {
		private String uri;

		private String username;

		private String password;

		private boolean skipSslValidation;

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

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

		public boolean isSkipSslValidation() {
			return skipSslValidation;
		}

		public void setSkipSslValidation(boolean skipSslValidation) {
			this.skipSslValidation = skipSslValidation;
		}
	}
}
