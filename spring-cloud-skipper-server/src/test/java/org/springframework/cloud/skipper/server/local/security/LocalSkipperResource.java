/*
 * Copyright 2016-2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.local.security;

import java.util.Collection;

import javax.servlet.Filter;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.skipper.server.local.security.skipperapp.LocalTestSkipperServer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
public class LocalSkipperResource extends ExternalResource {

	private final Logger LOGGER = LoggerFactory.getLogger(LocalSkipperResource.class);

	private SpringApplication app;

	private MockMvc mockMvc;

	private String skipperPort;

	private final String[] configLocations;
	private final String[] configNames;

	private final String[] args;

	private ConfigurableApplicationContext configurableApplicationContext;

	public LocalSkipperResource(String[] configLocations, String[] configNames, String[] args) {
		this.configLocations = configLocations;
		this.configNames = configNames;
		this.args = args;
	}

	public LocalSkipperResource(String[] configLocations, String[] configNames) {
		this(configLocations, configNames, new String[] { "--server.port=0" });
	}

	@Override
	protected void before() {

		final SpringApplicationBuilder builder = new SpringApplicationBuilder(LocalTestSkipperServer.class);

		builder.properties("spring.main.allow-bean-definition-overriding:true");

		if (this.configLocations  != null && this.configLocations.length > 0) {
			builder.properties(
				String.format("spring.config.additional-location:%s", StringUtils.arrayToCommaDelimitedString(this.configLocations))
			);
		}

		if (this.configNames  != null && this.configNames.length > 0) {
			builder.properties(
				String.format("spring.config.name:%s", StringUtils.arrayToCommaDelimitedString(this.configNames))
			);
		}

		this.app = builder.build();

		configurableApplicationContext = app.run(this.args);

		Collection<Filter> filters = configurableApplicationContext.getBeansOfType(Filter.class).values();
		mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) configurableApplicationContext)
				.addFilters(filters.toArray(new Filter[filters.size()])).build();
		skipperPort = configurableApplicationContext.getEnvironment().resolvePlaceholders("${server.port}");
		LOGGER.info("Skipper Server is UP on port {}!", skipperPort);
	}

	@Override
	protected void after() {
		SpringApplication.exit(configurableApplicationContext);
	}

	public MockMvc getMockMvc() {
		return mockMvc;
	}

	public String getSkipperPort() {
		return skipperPort;
	}

	public ConfigurableApplicationContext getWebApplicationContext() {
		return configurableApplicationContext;
	}

}
