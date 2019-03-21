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
package org.springframework.cloud.dataflow.server.config.cloudfoundry.security;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.cloud.common.security.OAuthSecurityConfiguration;
import org.springframework.cloud.common.security.support.DefaultAuthoritiesExtractor;
import org.springframework.cloud.common.security.support.OnOAuth2SecurityEnabled;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.security.support.CloudFoundryDataflowAuthoritiesExtractor;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.security.support.CloudFoundryPrincipalExtractor;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.security.support.CloudFoundrySecurityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

/**
 * When running inside Cloud Foundry, this {@link Configuration} class will reconfigure
 * Spring Cloud Data Flow's security setup in {@link OAuthSecurityConfiguration}, so that
 * only users with the CF_SPACE_DEVELOPER_ROLE} can access the REST APIs.
 * <p>
 * Therefore, this configuration will ensure that only Cloud Foundry
 * {@code Space Developers} have access to the underlying REST API's.
 * <p>
 * For this to happen, a REST call will be made to the Cloud Foundry Permissions API via
 * CloudFoundrySecurityService inside the {@link DefaultAuthoritiesExtractor}.
 * <p>
 * If the user has the respective permissions, the CF_SPACE_DEVELOPER_ROLE will be
 * assigned to the user.
 * <p>
 * See also:
 * https://apidocs.cloudfoundry.org/258/apps/retrieving_permissions_on_a_app.html
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
@Conditional(OnOAuth2SecurityEnabled.class)
@Import(CloudFoundryOAuthSecurityConfiguration.CloudFoundryUAAConfiguration.class)
public class CloudFoundryOAuthSecurityConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryOAuthSecurityConfiguration.class);

	@Autowired
	private UserInfoTokenServices userInfoTokenServices;

	@Autowired(required = false)
	private CloudFoundryDataflowAuthoritiesExtractor cloudFoundryDataflowAuthoritiesExtractor;

	@Autowired(required = false)
	private PrincipalExtractor principalExtractor;

	@PostConstruct
	public void init() {
		if (this.cloudFoundryDataflowAuthoritiesExtractor != null) {
			logger.info("Setting up Cloud Foundry AuthoritiesExtractor for UAA.");
			this.userInfoTokenServices.setAuthoritiesExtractor(this.cloudFoundryDataflowAuthoritiesExtractor);
		}
		if (this.principalExtractor != null) {
			logger.info("Setting up Cloud Foundry PrincipalExtractor.");
			this.userInfoTokenServices.setPrincipalExtractor(this.principalExtractor);
		}
		else {
			this.userInfoTokenServices.setPrincipalExtractor(new CloudFoundryPrincipalExtractor());
		}
	}

	@Configuration
	@ConditionalOnProperty(name = "spring.cloud.dataflow.security.cf-use-uaa", havingValue = "true")
	public class CloudFoundryUAAConfiguration {

		@Value("${vcap.application.cf_api}")
		private String cloudControllerUrl;

		@Value("${vcap.application.application_id}")
		private String applicationId;

		@Autowired
		private OAuth2RestTemplate oAuth2RestTemplate;

		@Bean
		public CloudFoundryDataflowAuthoritiesExtractor authoritiesExtractor() {
			return new CloudFoundryDataflowAuthoritiesExtractor(cloudFoundrySecurityService());
		}

		@Bean
		public CloudFoundrySecurityService cloudFoundrySecurityService() {
			return new CloudFoundrySecurityService(this.oAuth2RestTemplate, this.cloudControllerUrl,
					this.applicationId);
		}

	}

}
