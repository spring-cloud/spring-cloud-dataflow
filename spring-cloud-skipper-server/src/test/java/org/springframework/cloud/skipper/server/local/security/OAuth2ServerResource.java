/*
 * Copyright 2017-2019 the original author or authors.
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

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.skipper.server.local.security.support.oauth2testserver.OAuth2TestServerApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.SocketUtils;

/**
 * Bootstraps an embedded OAuth2 server using {@link OAuth2TestServerApplication}.
 *
 * @author Gunnar Hillert
 */
public class OAuth2ServerResource extends ExternalResource {

	private static final String OAUTH2_PORT_PROPERTY = "oauth2.port";

	private final Logger LOGGER = LoggerFactory.getLogger(OAuth2ServerResource.class);

	private String originalOAuth2Port;

	private int oauth2ServerPort;

	private ConfigurableApplicationContext application;

	public OAuth2ServerResource() {
		super();
	}

	@Override
	protected void before() throws Throwable {

		originalOAuth2Port = System.getProperty(OAUTH2_PORT_PROPERTY);

		this.oauth2ServerPort = SocketUtils.findAvailableTcpPort();

		LOGGER.info("Setting OAuth2 Server port to " + this.oauth2ServerPort);

		System.setProperty(OAUTH2_PORT_PROPERTY, String.valueOf(this.oauth2ServerPort));

		this.application = new SpringApplicationBuilder(OAuth2TestServerApplication.class)
				.properties("spring.main.allow-bean-definition-overriding:true")
				.build()
				.run(
					"--spring.cloud.common.security.enabled=false",
					"--spring.cloud.skipper.server.enabled=false",
					"--spring.config.additional-location=classpath:/org/springframework/cloud/skipper/server/local/security"
						+ "/support/oauth2TestServerConfig.yml");
		LOGGER.info("OAuth Server is UP on port {}!", this.oauth2ServerPort);
	}

	@Override
	protected void after() {
		try {
			application.stop();
		}
		finally {
			if (originalOAuth2Port != null) {
				System.setProperty(OAUTH2_PORT_PROPERTY, originalOAuth2Port);
			}
			else {
				System.clearProperty(OAUTH2_PORT_PROPERTY);
			}
		}
	}

	public int getOauth2ServerPort() {
		return oauth2ServerPort;
	}

}
