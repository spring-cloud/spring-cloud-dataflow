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

package org.springframework.cloud.dataflow.admin.spi.local;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.dataflow.admin.AdminApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Tests for {@link AdminApplication}.
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
}
