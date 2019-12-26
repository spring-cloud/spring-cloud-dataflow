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

package org.springframework.cloud.dataflow.integration.test;

import java.net.URI;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.integration.test.util.Wait;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Tzolov
 */
public class DockerComposeTestPlatform {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeTestPlatform.class);

	private static DataFlowTemplate dataFlowOperations;

	@BeforeClass
	public static void beforeClass() {
		dataFlowOperations = new DataFlowTemplate(URI.create("http://localhost:9393"));
		logger.info("Configured platforms: " + dataFlowOperations.streamOperations().listPlatforms().stream()
				.map(d -> String.format("[%s:%s]", d.getName(), d.getType())).collect(Collectors.joining()));
	}

	@Before
	public void before() {
		Wait.on(dataFlowOperations.appRegistryOperations()).until(appRegistry ->
				appRegistry.list().getMetadata().getTotalElements() >= 68L);
	}

	@After
	public void after() {
		dataFlowOperations.streamOperations().destroyAll();
		dataFlowOperations.taskOperations().destroyAll();
	}

	@Test
	public void featureInfo() {
		logger.info("feature-info-test");
		AboutResource about = dataFlowOperations.aboutOperation().get();
		//if (isPrometheusPresent() || isInfluxPresent()) {
		//	assertTrue(about.getFeatureInfo().isGrafanaEnabled());
		//}
		assertTrue(about.getFeatureInfo().isAnalyticsEnabled());
		assertTrue(about.getFeatureInfo().isStreamsEnabled());
		assertTrue(about.getFeatureInfo().isTasksEnabled());
		assertFalse(about.getFeatureInfo().isSchedulesEnabled());
	}

	@Test
	public void appsCount() {
		logger.info("apps-count-test");
		assertThat(dataFlowOperations.appRegistryOperations().list().getMetadata().getTotalElements(),
				greaterThanOrEqualTo(68L));
	}
}
