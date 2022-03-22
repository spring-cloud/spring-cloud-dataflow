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
package org.springframework.cloud.dataflow.server.single.security;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientProperties;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.server.single.LocalDataflowResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gunnar Hillert
 */
public class DataFlowClientAutoConfigurationAgainstServerTests {

	private final static OAuth2ServerResource oAuth2ServerResource = new OAuth2ServerResource();

	private final static LocalDataflowResource localDataflowResource = new LocalDataflowResource(
			"classpath:org/springframework/cloud/dataflow/server/single/security/oauthConfig.yml");

	private AnnotationConfigApplicationContext context;

	@After
	public void clean() {
		if (context != null) {
			context.close();
		}
		context = null;
	}

	@ClassRule
	public static TestRule springDataflowAndOAuth2Server = RuleChain.outerRule(oAuth2ServerResource)
			.around(localDataflowResource);

	@Test
	public void usingUserWithAllRoles() throws Exception {

		final ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
		resourceDetails.setClientId("myclient");
		resourceDetails.setClientSecret("mysecret");
		resourceDetails.setUsername("user");
		resourceDetails.setPassword("secret10");
		resourceDetails
				.setAccessTokenUri("http://localhost:" + oAuth2ServerResource.getOauth2ServerPort() + "/oauth/token");

		final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);
		final OAuth2AccessToken accessToken = oAuth2RestTemplate.getAccessToken();

		System.setProperty("accessTokenAsString", accessToken.getValue());

		context = new AnnotationConfigApplicationContext(TestApplication.class);

		final DataFlowOperations dataFlowOperations = context.getBean(DataFlowOperations.class);
		final AboutResource about = dataFlowOperations.aboutOperation().get();

		assertNotNull(about);
		assertEquals("user", about.getSecurityInfo().getUsername());
		assertEquals(7, about.getSecurityInfo().getRoles().size());
	}

	@Test
	public void usingUserWithViewRoles() throws Exception {

		final ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
		resourceDetails.setClientId("myclient");
		resourceDetails.setClientSecret("mysecret");
		resourceDetails.setUsername("bob");
		resourceDetails.setPassword("bobspassword");
		resourceDetails
				.setAccessTokenUri("http://localhost:" + oAuth2ServerResource.getOauth2ServerPort() + "/oauth/token");

		final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);
		final OAuth2AccessToken accessToken = oAuth2RestTemplate.getAccessToken();

		System.setProperty("accessTokenAsString", accessToken.getValue());

		context = new AnnotationConfigApplicationContext(TestApplication.class);

		final DataFlowOperations dataFlowOperations = context.getBean(DataFlowOperations.class);
		final AboutResource about = dataFlowOperations.aboutOperation().get();

		assertNotNull(about);
		assertEquals("bob", about.getSecurityInfo().getUsername());
		assertEquals(1, about.getSecurityInfo().getRoles().size());
	}

	@Test
	public void usingUserWithViewRolesWithOauth() {
		context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(
				"spring.cloud.dataflow.client.server-uri=" + "http://localhost:"
						+ localDataflowResource.getDataflowPort(),
				"spring.cloud.dataflow.client.authentication.client-id=myclient",
				"spring.cloud.dataflow.client.authentication.client-secret=mysecret",
				"spring.cloud.dataflow.client.authentication.token-uri=" + "http://localhost:"
						+ oAuth2ServerResource.getOauth2ServerPort() + "/oauth/token",
				"spring.cloud.dataflow.client.authentication.scope=dataflow.view").applyTo(context);
		context.register(TestApplication2.class);
		context.refresh();

		final DataFlowOperations dataFlowOperations = context.getBean(DataFlowOperations.class);
		final AboutResource about = dataFlowOperations.aboutOperation().get();

		assertNotNull(about);
		assertEquals("myclient", about.getSecurityInfo().getUsername());
		assertEquals(1, about.getSecurityInfo().getRoles().size());
	}

	@Import(DataFlowClientAutoConfiguration.class)
	static class TestApplication {
		@Bean
		@Primary
		DataFlowClientProperties dataFlowClientProperties() {
			final String accessTokenAsString = System.getProperty("accessTokenAsString");
			final DataFlowClientProperties dataFlowClientProperties = new DataFlowClientProperties();
			dataFlowClientProperties.getAuthentication().setAccessToken(accessTokenAsString);
			dataFlowClientProperties.setServerUri("http://localhost:" + localDataflowResource.getDataflowPort());
			return dataFlowClientProperties;
		}
	}

	@Import(DataFlowClientAutoConfiguration.class)
	static class TestApplication2 {
	}
}
