/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.single;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;

/**
 * Bootstrap class for the Spring Cloud Data Flow Server.
 *
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
@SpringBootApplication(exclude = {
		SessionAutoConfiguration.class,
		SimpleTaskAutoConfiguration.class,
		ManagementWebSecurityAutoConfiguration.class,
		SecurityAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class,
		LocalDeployerAutoConfiguration.class,
		CloudFoundryDeployerAutoConfiguration.class,
		KubernetesAutoConfiguration.class
})
@EnableDataFlowServer
public class DataFlowServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataFlowServerApplication.class, args);
	}
}
