/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class StreamCommandTests extends AbstractShellIntegrationTest {

	private static final String APPS_URI = "META-INF/test-stream-apps.properties";

	private static final Logger logger = LoggerFactory.getLogger(StreamCommandTests.class);

	@Before
	public void registerApps() {
		AppRegistry registry = applicationContext.getBean(AppRegistry.class);
		registry.importAll(true, new ClassPathResource(APPS_URI));
	}

	@Test
	public void testStreamLifecycleForTickTock() throws InterruptedException {
		logger.info("Starting Stream Test for TickTock");
		String streamName = generateUniqueName();
		stream().create(streamName, "time | log");
	}
}
