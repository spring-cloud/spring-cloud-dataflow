/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.local;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Tests for {@link LocalDeployerAutoConfiguration}.
 *
 * @author Janne Valkealahti
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class LocalConfigurationTests {

	private static final String APP_DEPLOYER_BEAN_NAME = "appDeployer";

	private static final String TASK_LAUNCHER_BEAN_NAME = "taskLauncher";

	@Test
	public void testConfig() {
		SpringApplication app = new SpringApplication(LocalDataFlowServer.class);
		ConfigurableApplicationContext context = app.run(new String[] { "--server.port=0" });
		assertThat(context.containsBean(APP_DEPLOYER_BEAN_NAME), is(true));
		assertThat(context.getBean(APP_DEPLOYER_BEAN_NAME), instanceOf(LocalAppDeployer.class));
		assertThat(context.containsBean(TASK_LAUNCHER_BEAN_NAME), is(true));
		assertThat(context.getBean(TASK_LAUNCHER_BEAN_NAME), instanceOf(LocalTaskLauncher.class));
		context.close();
	}

}
