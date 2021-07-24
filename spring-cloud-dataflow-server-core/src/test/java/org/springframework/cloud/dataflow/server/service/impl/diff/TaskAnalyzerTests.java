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
package org.springframework.cloud.dataflow.server.service.impl.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class TaskAnalyzerTests {

	private final TaskAnalyzer analyzer = new TaskAnalyzer();

	@Test
	public void testDeploymentProperties() {
		AppDefinition leftAd = new AppDefinition("name1", new HashMap<>());
		Resource leftResource = new ClassPathResource("path1");
		Map<String, String> leftDeploymentProperties = new HashMap<>();
		leftDeploymentProperties.put("key1", "value1");
		List<String> leftCommandlineArguments = new ArrayList<>();
		AppDeploymentRequest leftAdr = new AppDeploymentRequest(leftAd, leftResource, leftDeploymentProperties,
				leftCommandlineArguments);
		TaskManifest leftManifest = new TaskManifest();
		leftManifest.setPlatformName("default");
		leftManifest.setTaskDeploymentRequest(leftAdr);

		AppDefinition rightAd = new AppDefinition("name2", new HashMap<>());
		Resource rightResource = new ClassPathResource("path2");
		Map<String, String> rightDeploymentProperties = new HashMap<>();
		rightDeploymentProperties.put("key1", "value1");
		List<String> rightCommandlineArguments = new ArrayList<>();
		AppDeploymentRequest rightAdr = new AppDeploymentRequest(rightAd, rightResource, rightDeploymentProperties,
				rightCommandlineArguments);
		TaskManifest rightManifest = new TaskManifest();
		rightManifest.setPlatformName("default");
		rightManifest.setTaskDeploymentRequest(rightAdr);

		TaskAnalysisReport report = analyzer.analyze(leftManifest, rightManifest);
		TaskManifestDifference taskManifestDifference = report.getTaskManifestDifference();
		PropertiesDiff deploymentPropertiesDifference = taskManifestDifference.getDeploymentPropertiesDifference();
		assertThat(deploymentPropertiesDifference.areEqual()).isTrue();

		rightDeploymentProperties.put("key1", "value2");

		report = analyzer.analyze(leftManifest, rightManifest);
		taskManifestDifference = report.getTaskManifestDifference();
		deploymentPropertiesDifference = taskManifestDifference.getDeploymentPropertiesDifference();
		assertThat(deploymentPropertiesDifference.areEqual()).isFalse();
		assertThat(report.getMergedDeploymentProperties()).hasSize(1);
		assertThat(report.getMergedDeploymentProperties()).contains(entry("key1", "value2"));
	}
	
	
	@Test
	public void testAnalyze() {

		Map<String, String> leftDeploymentProperties = new HashMap<>();
		leftDeploymentProperties.put("key1", "value1");

		Map<String, String> rightDeploymentProperties = new HashMap<>();
		rightDeploymentProperties.put("key1", "value1");

		TaskAnalysisReport report = analyzer.analyze(leftDeploymentProperties, rightDeploymentProperties);
		TaskManifestDifference taskManifestDifference = report.getTaskManifestDifference();
		PropertiesDiff deploymentPropertiesDifference = taskManifestDifference.getDeploymentPropertiesDifference();
		assertThat(deploymentPropertiesDifference.areEqual()).isTrue();

		rightDeploymentProperties.put("key1", "value2");

		report = analyzer.analyze(leftDeploymentProperties, rightDeploymentProperties);
		taskManifestDifference = report.getTaskManifestDifference();
		deploymentPropertiesDifference = taskManifestDifference.getDeploymentPropertiesDifference();
		assertThat(deploymentPropertiesDifference.areEqual()).isFalse();
		assertThat(report.getMergedDeploymentProperties()).hasSize(1);
		assertThat(report.getMergedDeploymentProperties()).contains(entry("key1", "value2"));
	}

}
