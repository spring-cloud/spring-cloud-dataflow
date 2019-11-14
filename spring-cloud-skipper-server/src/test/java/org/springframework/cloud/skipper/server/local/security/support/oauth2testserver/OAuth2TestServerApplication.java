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
package org.springframework.cloud.skipper.server.local.security.support.oauth2testserver;

import org.springframework.boot.actuate.autoconfigure.flyway.FlywayEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.cloud.skipper.server.autoconfigure.SkipperServerAutoConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.statemachine.boot.autoconfigure.StateMachineAutoConfiguration;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;


/**
 *
 * @author Gunnar Hillert
 *
 */
@EnableResourceServer
@EnableConfigurationProperties({FileSecurityProperties.class})
@SpringBootApplication(
		excludeName = {
				"org.springframework.cloud.dataflow.shell.autoconfigure.BaseShellAutoConfiguration" },
		exclude = {
				ServiceRegistryAutoConfiguration.class,
				JpaRepositoriesAutoConfiguration.class,
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
				FlywayAutoConfiguration.class,
				SpringDataWebAutoConfiguration.class,
				StateMachineJpaRepositoriesAutoConfiguration.class,
				TaskExecutionAutoConfiguration.class,
				TaskSchedulingAutoConfiguration.class,
				FlywayEndpointAutoConfiguration.class,
				RepositoryRestMvcAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class,
				LocalDeployerAutoConfiguration.class,
				StateMachineAutoConfiguration.class,
				SkipperServerAutoConfiguration.class,
				IntegrationAutoConfiguration.class })
public class OAuth2TestServerApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(OAuth2TestServerApplication.class)
		.run(
				"--spring.cloud.common.security.enabled=false",
				"--spring.cloud.skipper.server.enabled=false",
				"--server.port=9999",
				"--logging.level.org.springframework=debug",
				"--spring.cloud.kubernetes.enabled=false",
				"--spring.config.location=classpath:org/springframework/cloud/skipper/server/local/security/support/oauth2TestServerConfig.yml"
				);
	}

}
