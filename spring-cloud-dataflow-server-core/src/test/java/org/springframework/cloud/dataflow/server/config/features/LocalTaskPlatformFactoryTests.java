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

package org.springframework.cloud.dataflow.server.config.features;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Corneil du Plessis
 **/
public class LocalTaskPlatformFactoryTests {

	@Test
	public void createsDefaultPlatform() {
		LocalPlatformProperties platformProperties = new LocalPlatformProperties();
		LocalTaskPlatformFactory taskPlatformFactory = new LocalTaskPlatformFactory(platformProperties, null);
		TaskPlatform taskPlatform = taskPlatformFactory.createTaskPlatform();
		assertThat(taskPlatform.getLaunchers()).hasSize(1);
		assertThat(taskPlatform.getName()).isEqualTo("Local");
		assertThat(taskPlatform.getLaunchers().get(0).getType()).isEqualTo(taskPlatform.getName());
		assertThat(taskPlatform.getLaunchers().get(0).getName()).isEqualTo("default");
		assertThat(taskPlatform.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(LocalTaskLauncher.class);
		assertThat(taskPlatform.getLaunchers().get(0).getDescription()).isNotEmpty();
	}

	@Test
	public void createsConfiguredPlatform() {
		LocalPlatformProperties platformProperties = new LocalPlatformProperties();
		platformProperties.setAccounts(Collections.singletonMap("custom",new LocalDeployerProperties()));
		LocalTaskPlatformFactory taskPlatformFactory = new LocalTaskPlatformFactory(platformProperties, null);
		TaskPlatform taskPlatform = taskPlatformFactory.createTaskPlatform();
		assertThat(taskPlatform.getLaunchers()).hasSize(1);
		assertThat(taskPlatform.getName()).isEqualTo("Local");
		assertThat(taskPlatform.getLaunchers().get(0).getType()).isEqualTo(taskPlatform.getName());
		assertThat(taskPlatform.getLaunchers().get(0).getName()).isEqualTo("custom");
		assertThat(taskPlatform.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(LocalTaskLauncher.class);
		assertThat(taskPlatform.getLaunchers().get(0).getDescription()).isNotEmpty();
	}

}
