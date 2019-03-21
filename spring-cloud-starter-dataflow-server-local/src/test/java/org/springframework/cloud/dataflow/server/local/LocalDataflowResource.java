/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.local;

import java.util.Collection;

import javax.servlet.Filter;

import org.junit.rules.ExternalResource;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.local.dataflowapp.LocalTestDataFlowServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Marius Bogoevici
 * @author Gunnar Hillert
 */
public class LocalDataflowResource extends ExternalResource {

	final boolean streamsEnabled;

	final boolean tasksEnabled;

	final boolean metricsEnabled;

	final boolean schedulerEnabled;

	final boolean skipperEnabled;

	private String originalConfigLocation = null;

	private SpringApplication app;

	private MockMvc mockMvc;

	private String dataflowPort;

	private String skipperServerPort;

	private String configurationLocation;

	private WebApplicationContext configurableApplicationContext;

	public LocalDataflowResource(String configurationLocation) {
		this.configurationLocation = configurationLocation;
		this.streamsEnabled = true;
		this.tasksEnabled = true;
		this.metricsEnabled = true;
		this.schedulerEnabled = false;
		this.skipperEnabled = false;
	}

	public LocalDataflowResource(String configurationLocation, boolean streamsEnabled, boolean tasksEnabled) {
		this.configurationLocation = configurationLocation;
		this.streamsEnabled = streamsEnabled;
		this.tasksEnabled = tasksEnabled;
		this.metricsEnabled = true;
		this.schedulerEnabled = false;
		this.skipperEnabled = false;
	}

	public LocalDataflowResource(String configurationLocation, boolean streamsEnabled, boolean tasksEnabled, boolean metricsEnabled) {
		this.configurationLocation = configurationLocation;
		this.streamsEnabled = streamsEnabled;
		this.tasksEnabled = tasksEnabled;
		this.metricsEnabled = metricsEnabled;
		this.schedulerEnabled = false;
		this.skipperEnabled = false;
	}

	public LocalDataflowResource(String configurationLocation, boolean streamsEnabled, boolean tasksEnabled, boolean metricsEnabled, boolean schedulerEnabled) {
		this.configurationLocation = configurationLocation;
		this.streamsEnabled = streamsEnabled;
		this.tasksEnabled = tasksEnabled;
		this.metricsEnabled = metricsEnabled;
		this.schedulerEnabled = schedulerEnabled;
		this.skipperEnabled = false;
	}

	public LocalDataflowResource(String configurationLocation, boolean streamsEnabled, boolean tasksEnabled,
			boolean metricsEnabled, boolean schedulerEnabled, boolean skipperEnabled, String skipperServerPort) {
		this.configurationLocation = configurationLocation;
		this.streamsEnabled = streamsEnabled;
		this.tasksEnabled = tasksEnabled;
		this.metricsEnabled = metricsEnabled;
		this.schedulerEnabled = schedulerEnabled;
		this.skipperEnabled = skipperEnabled;
		this.skipperServerPort = skipperServerPort;
	}

	@Override
	protected void before() throws Throwable {
		originalConfigLocation = System.getProperty("spring.config.location");

		if (!StringUtils.isEmpty(configurationLocation)) {
			System.setProperty("spring.config.location", configurationLocation);
		}

		app = new SpringApplication(LocalTestDataFlowServer.class);

		configurableApplicationContext = (WebApplicationContext) app.run(new String[] { "--server.port=0",
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.STREAMS_ENABLED + "="
						+ this.streamsEnabled,
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.TASKS_ENABLED + "="
						+ this.tasksEnabled,
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.ANALYTICS_ENABLED + "="
						+ this.metricsEnabled,
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.SCHEDULES_ENABLED + "="
						+ this.schedulerEnabled,
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.SKIPPER_ENABLED + "="
						+ this.skipperEnabled,
				"--spring.cloud.skipper.client.serverUri=http://localhost:" + this.skipperServerPort + "/api"
		});

		Collection<Filter> filters = configurableApplicationContext.getBeansOfType(Filter.class).values();
		mockMvc = MockMvcBuilders.webAppContextSetup(configurableApplicationContext)
				.addFilters(filters.toArray(new Filter[filters.size()])).build();
		dataflowPort = configurableApplicationContext.getEnvironment().resolvePlaceholders("${server.port}");
	}

	@Override
	protected void after() {
		SpringApplication.exit(configurableApplicationContext);
		resetConfigLocation();
	}

	private void resetConfigLocation() {
		if (originalConfigLocation != null) {
			System.setProperty("spring.config.location", originalConfigLocation);
		}
		else {
			System.clearProperty("spring.config.location");
		}
	}

	public MockMvc getMockMvc() {
		return mockMvc;
	}

	public String getDataflowPort() {
		return dataflowPort;
	}

	public WebApplicationContext getWebApplicationContext() {
		return configurableApplicationContext;
	}

}
