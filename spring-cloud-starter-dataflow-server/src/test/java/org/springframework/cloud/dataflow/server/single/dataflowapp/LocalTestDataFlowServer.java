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

package org.springframework.cloud.dataflow.server.single.dataflowapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;

/**
 * Bootstrap class for the local Spring Cloud Data Flow Server.
 * <p>
 * Multiple SpringBootApplication's needs to be in their own directories due to component
 * scanning.
 *
 * @author Mark Fisher
 */
@SpringBootApplication(exclude = {
		SessionAutoConfiguration.class,
		ManagementWebSecurityAutoConfiguration.class,
		SecurityAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class,
		LocalDeployerAutoConfiguration.class,
		CloudFoundryDeployerAutoConfiguration.class,
		KubernetesAutoConfiguration.class,
		SimpleTaskAutoConfiguration.class,
		DataFlowClientAutoConfiguration.class
})
@EnableDataFlowServer
public class LocalTestDataFlowServer {
	public static void main(String[] args) {
		SpringApplication.run(LocalTestDataFlowServer.class, new String[] {
				"--spring.cloud.kubernetes.enabled=false",
				"--security.oauth2.client.client-id=myclient",
				"--security.oauth2.client.client-secret=mysecret",
				"--security.oauth2.client.access-token-uri=http://127.0.0.1:9999/oauth/token",
				"--security.oauth2.client.user-authorization-uri=http://127.0.0.1:9999/oauth/authorize",
				"--security.oauth2.resource.user-info-uri=http://127.0.0.1:9999/me"});
	}
}
