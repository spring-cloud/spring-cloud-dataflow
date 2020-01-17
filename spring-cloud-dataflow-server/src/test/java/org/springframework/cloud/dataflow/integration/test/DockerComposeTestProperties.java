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

package org.springframework.cloud.dataflow.integration.test;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties(prefix = "test.docker.compose")
public class DockerComposeTestProperties {

	/**
	 * Default url to connect to dataflow
	 */
	private String dataflowServerUrl = "http://localhost:9393";

	/**
	 * default - local platform (e.g. docker-compose)
	 * cf - Cloud Foundry platform, configured in docker-compose-cf.yml
	 * k8s - GKE/Kubernetes platform, configured via docker-compose-k8s.yml.
	 */
	private String platformName = "default";

	/**
	 * Default url to connect to SCDF's Prometheus TSDB
	 */
	private String prometheusUrl = "http://localhost:9090";

	/**
	 * Default url to connect to SCDF's Influx TSDB
	 */
	private String influxUrl = "http://localhost:8086";

	/**
	 * Kubernetes tests require nginx-ingress and watchdog mechanism to expose the apps  URL
	 * to public access. Later requires a preconfigured ingress servers domain name suffix.
	 *
	 * For example the Hydra AT environment hash the 'hydra.springapps.io' host name and app Urls
	 * will have the form  https://partitioning-test-log-v1.hydra.springapps.io
	 * or  https://partitioning-test-log-v1-1.hydra.springapps.io for multiple instance.
	 */
	private String kubernetesAppHostSuffix = "";

	public String getDataflowServerUrl() {
		return dataflowServerUrl;
	}

	public void setDataflowServerUrl(String dataflowServerUrl) {
		this.dataflowServerUrl = dataflowServerUrl;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public String getPrometheusUrl() {
		return prometheusUrl;
	}

	public void setPrometheusUrl(String prometheusUrl) {
		this.prometheusUrl = prometheusUrl;
	}

	public String getInfluxUrl() {
		return influxUrl;
	}

	public void setInfluxUrl(String influxUrl) {
		this.influxUrl = influxUrl;
	}

	public String getKubernetesAppHostSuffix() {
		return kubernetesAppHostSuffix;
	}

	public void setKubernetesAppHostSuffix(String kubernetesAppHostSuffix) {
		this.kubernetesAppHostSuffix = kubernetesAppHostSuffix;
	}
}
