/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.rest.resource.about.MonitoringDashboardType;
import org.springframework.util.StringUtils;

/**
 * A common property bag for all supported Dashboard monitoring systems (wavefront and grafana at the moment).
 * @author Christian Tzolov
 */
@ConfigurationProperties(prefix = MonitoringDashboardInfoProperties.VERSION_INFO_PREFIX)
public class MonitoringDashboardInfoProperties {

	public static final String VERSION_INFO_PREFIX = DataFlowPropertyKeys.PREFIX + "monitoring-info";

	/**
	 * Root URL to access the monitoring dashboards
	 */
	private String url = "";

	/**
	 * The type of the Monitoring dashboard those properties are provided for.
	 */
	private MonitoringDashboardType dashboardType = MonitoringDashboardType.NONE;

	private Wavefront wavefront = new Wavefront();

	private Grafana grafana = new Grafana();

	public static class Grafana {
		/**
		 * If provided, can be used to authenticate with Grafana.
		 * https://docs.grafana.org/http_api/auth/#create-api-token
		 *
		 * The 'Authorization' header value should be: 'Bearer (your api key)'.
		 */
		private String token = "";

		/**
		 * Dashboard refresh interval in Seconds
		 */
		private int refreshInterval = 15;

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		public int getRefreshInterval() {
			return refreshInterval;
		}

		public void setRefreshInterval(int refreshInterval) {
			this.refreshInterval = refreshInterval;
		}
	}

	public static class Wavefront {
		/**
		 * Unique identifier for Wavefront to know the metrics are coming from this Data Flow installation.
		 */
		@Value("${management.metrics.export.wavefront.source:default-scdf-source}")
		private String source = "";

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public static String getVersionInfoPrefix() {
		return VERSION_INFO_PREFIX;
	}

	public MonitoringDashboardType getDashboardType() {
		return dashboardType;
	}

	public void setDashboardType(MonitoringDashboardType dashboardType) {
		this.dashboardType = dashboardType;
	}

	public boolean isEnabled() {
		return StringUtils.hasText(this.url) && this.getDashboardType() != MonitoringDashboardType.NONE;
	}

	public Wavefront getWavefront() {
		return this.wavefront;
	}

	public Grafana getGrafana() {
		return grafana;
	}
}
