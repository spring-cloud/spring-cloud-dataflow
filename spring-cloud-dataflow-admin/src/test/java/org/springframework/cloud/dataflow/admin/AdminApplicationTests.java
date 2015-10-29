/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.admin;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.dataflow.module.deployer.kubernetes.KubernetesModuleDeployer;
import org.springframework.cloud.dataflow.module.deployer.local.LocalModuleDeployer;
import org.springframework.cloud.dataflow.module.deployer.yarn.YarnModuleDeployer;
import org.springframework.cloud.lattice.LatticeProperties;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Generic tests for {@link AdminApplication}.
 *
 * @author Janne Valkealahti
 */
public class AdminApplicationTests {

	@Test
	public void testDefaultProfile() {
		SpringApplication app = new SpringApplication(AdminApplication.class);
		ConfigurableApplicationContext context = app.run(new String[] { "--server.port=0" });
		assertThat(context.containsBean("processModuleDeployer"), is(true));
		assertThat(context.getBean("processModuleDeployer"), instanceOf(LocalModuleDeployer.class));
		context.close();
	}

	@Test
	public void testYarnProfile() {
		SpringApplication app = new SpringApplication(AdminApplication.class);
		ConfigurableApplicationContext context = app.run(new String[] { "--spring.profiles.active=yarn",
				"--server.port=0" });
		assertThat(context.containsBean("processModuleDeployer"), is(true));
		assertThat(context.getBean("processModuleDeployer"), instanceOf(YarnModuleDeployer.class));
		context.close();
	}

	@Test
	public void testLatticeProfile() {
		SpringApplication app = new SpringApplication(AdminApplication.class);
		ConfigurableApplicationContext context = app.run(new String[] { "--spring.profiles.active=lattice",
				"--server.port=0" , "--spring.cloud.lattice.receptor.host=receptor.52.7.176.225.xip.io"});
		assertThat(context.containsBean("latticeProperties"), is(true));
		assertThat(context.getBean("latticeProperties", LatticeProperties.class).getReceptor().getHost(), is("receptor.52.7.176.225.xip.io"));
		context.close();
	}
	
	@Test
	public void testKubernetesProfile() {
		SpringApplication app = new SpringApplication(AdminApplication.class);
		ConfigurableApplicationContext context = app.run(new String[] { "--spring.profiles.active=kubernetes",
				"--server.port=-1" });
		assertThat(context.containsBean("processModuleDeployer"), is(true));
		assertThat(context.getBean("processModuleDeployer"), instanceOf(KubernetesModuleDeployer.class));
		context.close();
	}

}
