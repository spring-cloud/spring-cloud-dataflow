/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.local;

import java.util.Collection;

import org.junit.rules.ExternalResource;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.local.dataflowapp.LocalTestDataFlowServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import javax.servlet.Filter;

/**
 * @author Marius Bogoevici
 * @author Gunnar Hillert
 */
public class LocalDataflowResource extends ExternalResource {

	private String originalConfigLocation = null;

	private SpringApplication app;

	private MockMvc mockMvc;

	private String dataflowPort;

	private String configurationLocation;

	private WebApplicationContext configurableApplicationContext;

	public LocalDataflowResource(String configurationLocation) {
		this.configurationLocation = configurationLocation;
	}

	@Override
	protected void before() throws Throwable {
		originalConfigLocation = System.getProperty("spring.config.location");
		if (!StringUtils.isEmpty(configurationLocation)) {
			System.setProperty("spring.config.location", configurationLocation);
		}

		app = new SpringApplication(LocalTestDataFlowServer.class);
		configurableApplicationContext = (WebApplicationContext) app.run(new String[]{"--server.port=0",
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.STREAMS_ENABLED + "=true",
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.ANALYTICS_ENABLED + "=true"});

		Collection<Filter> filters = configurableApplicationContext.getBeansOfType(Filter.class).values();
		mockMvc = MockMvcBuilders.webAppContextSetup(configurableApplicationContext)
				.addFilters(filters.toArray(new Filter[filters.size()]))
				.build();
		dataflowPort = configurableApplicationContext.getEnvironment().resolvePlaceholders("${server.port}");
	}

	@Override
	protected void after() {
		SpringApplication.exit(configurableApplicationContext);
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

