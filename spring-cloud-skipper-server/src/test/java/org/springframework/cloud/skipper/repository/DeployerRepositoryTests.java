/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.repository;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.skipper.AbstractIntegrationTest;
import org.springframework.cloud.skipper.deployer.Deployer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
public class DeployerRepositoryTests extends AbstractIntegrationTest {

	@Autowired
	private DeployerRepository deployerRepository;

	@Test
	public void basicCrud() {
		LocalAppDeployer localAppDeployer = new LocalAppDeployer(new LocalDeployerProperties());
		Deployer deployer = new Deployer("localDeployer", "local", localAppDeployer);
		deployerRepository.save(deployer);
		assertThat(deployerRepository.count()).isEqualTo(1);
		assertThat(deployer.getId()).isNotEmpty();
		assertThat(deployerRepository.findByName("localDeployer")).isNotNull();
	}
}
