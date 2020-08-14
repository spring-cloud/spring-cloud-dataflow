/*
 * Copyright 2019 the original author or authors.
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
import org.springframework.util.StringUtils;

/**
 * @author Christian Tzolov
 *
 * @deprecated use the {@link MonitoringDashboardInfoProperties} instead.
 */
@ConfigurationProperties(prefix = GrafanaInfoProperties.VERSION_INFO_PREFIX)
@Deprecated
public class GrafanaInfoProperties {

	public static final String VERSION_INFO_PREFIX = DataFlowPropertyKeys.PREFIX + "grafana-info";

	/**
	 * Root URL to access the grafana dashboards
	 */
	private String url = "";

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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

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

	public boolean isGrafanaEnabled() {
		return StringUtils.hasText(this.url);
	}
}
