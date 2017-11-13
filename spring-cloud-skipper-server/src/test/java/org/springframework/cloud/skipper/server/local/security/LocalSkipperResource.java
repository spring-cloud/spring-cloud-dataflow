/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 */
public class LocalSkipperResource extends ExternalResource {

	private SpringApplication app;

	private MockMvc mockMvc;

	private String skipperPort;

	private String[] configLocations;
	private String[] configNames;

	private ConfigurableApplicationContext configurableApplicationContext;

	public LocalSkipperResource(String[] configLocations, String[] configNames) {
		this.configLocations = configLocations;
		this.configNames = configNames;
	}

	@Override
	protected void before() throws Throwable {

		final SpringApplicationBuilder builder = new SpringApplicationBuilder(LocalTestSkipperServer.class);

		if (this.configLocations  != null && this.configLocations.length > 0) {
			builder.properties(
				String.format("spring.config.location:%s", StringUtils.arrayToCommaDelimitedString(this.configLocations))
			);
		}

		if (this.configNames  != null && this.configNames.length > 0) {
			builder.properties(
				String.format("spring.config.name:%s", StringUtils.arrayToCommaDelimitedString(this.configNames))
			);
		}

		this.app = builder.build();



		configurableApplicationContext = app.run(
			new String[] { "--server.port=0" });

		Collection<Filter> filters = configurableApplicationContext.getBeansOfType(Filter.class).values();
		mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) configurableApplicationContext)
				.addFilters(filters.toArray(new Filter[filters.size()])).build();
		skipperPort = configurableApplicationContext.getEnvironment().resolvePlaceholders("${server.port}");
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
