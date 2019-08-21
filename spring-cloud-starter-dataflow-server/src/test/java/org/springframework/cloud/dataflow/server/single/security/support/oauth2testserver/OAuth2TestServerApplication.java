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
package org.springframework.cloud.dataflow.server.single.security.support.oauth2testserver;

import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.dataflow.autoconfigure.local.LocalDataFlowServerAutoConfiguration;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolverAutoConfiguration;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;


/**
 *
 * @author Gunnar Hillert
 *
 */
@EnableResourceServer
@SpringBootApplication(
		excludeName = {
		"org.springframework.cloud.dataflow.shell.autoconfigure.BaseShellAutoConfiguration" },
		exclude = {
				CommonSecurityAutoConfiguration.class,
				SessionAutoConfiguration.class,
				ManagementWebSecurityAutoConfiguration.class,
				LocalDeployerAutoConfiguration.class,
				CloudFoundryDeployerAutoConfiguration.class,
				KubernetesAutoConfiguration.class,
				org.springframework.cloud.kubernetes.KubernetesAutoConfiguration.class,
				DataSourceAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				JmxAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class,
				LocalDataFlowServerAutoConfiguration.class,
				ApplicationConfigurationMetadataResolverAutoConfiguration.class,
				LocalDeployerAutoConfiguration.class,
				IntegrationAutoConfiguration.class })
public class OAuth2TestServerApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(OAuth2TestServerApplication.class)
		.run(
				"--spring.cloud.common.security.enabled=false",
				"--server.port=9999",
				"--logging.level.org.springframework=debug",
				"--spring.cloud.kubernetes.enabled=false",
				"--spring.config.location=classpath:/org/springframework/cloud/dataflow/server/single/security/support/oauth2testserver/oauth2TestServerConfig.yml"
				);
	}

}
