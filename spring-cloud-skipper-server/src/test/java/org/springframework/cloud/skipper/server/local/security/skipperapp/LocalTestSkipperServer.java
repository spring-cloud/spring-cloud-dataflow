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

package org.springframework.cloud.skipper.server.local.security.skipperapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.cloud.skipper.server.EnableSkipperServer;

/**
 * Bootstrap class for the local Spring Cloud Skipper Server.
 * <p>
 * Multiple SpringBootApplication's needs to be in their own directories due to component
 * scanning.
 *
 * @author Gunnar Hillert
 */
@SpringBootApplication(exclude = { CloudFoundryDeployerAutoConfiguration.class,
		LocalDeployerAutoConfiguration.class,
		KubernetesAutoConfiguration.class,
		SessionAutoConfiguration.class,
		org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class,
		org.springframework.boot.actuate.autoconfigure.ManagementWebSecurityAutoConfiguration.class })
@EnableSkipperServer
public class LocalTestSkipperServer {

	public static void main(String[] args) {
		SpringApplication.run(LocalTestSkipperServer.class, args);
	}
}
