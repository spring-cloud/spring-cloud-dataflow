/*
 * Copyright 2021-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.integration.test.IntegrationTestProperties;
import org.springframework.cloud.dataflow.integration.test.db.container.DataflowCluster;
import org.springframework.cloud.dataflow.integration.test.db.container.DataflowCluster.ClusterContainer;
import org.springframework.cloud.dataflow.integration.test.tags.TagNames;
import org.springframework.cloud.dataflow.integration.test.util.AssertUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Base test class for all database integration tests providing foundation for
 * images and basic features.
 *
 * @author Janne Valkealahti
 */
@Testcontainers
@SpringBootTest(classes = {AbstractDataflowTests.EmptyConfig.class})
@EnableConfigurationProperties({IntegrationTestProperties.class})
public abstract class AbstractDataflowTests {

	@Configuration
	protected static class EmptyConfig {
	}

	public final static String DATAFLOW_IMAGE_PREFIX = "springcloud/spring-cloud-dataflow-server:";

	public final static String SKIPPER_IMAGE_PREFIX = "springcloud/spring-cloud-skipper-server:";

	public final static List<ClusterContainer> DATAFLOW_CONTAINERS = Arrays.asList(
		ClusterContainer.from(TagNames.DATAFLOW_2_7, DATAFLOW_IMAGE_PREFIX + "2.7.2"),
		ClusterContainer.from(TagNames.DATAFLOW_2_8, DATAFLOW_IMAGE_PREFIX + "2.8.4"),
		ClusterContainer.from(TagNames.DATAFLOW_2_9, DATAFLOW_IMAGE_PREFIX + "2.9.6"),
		ClusterContainer.from(TagNames.DATAFLOW_2_10, DATAFLOW_IMAGE_PREFIX + "2.10.3"),
		ClusterContainer.from(TagNames.DATAFLOW_2_11, DATAFLOW_IMAGE_PREFIX + "2.11.2")
	);

	public final static List<ClusterContainer> SKIPPER_CONTAINERS = Arrays.asList(
		ClusterContainer.from(TagNames.SKIPPER_2_6, SKIPPER_IMAGE_PREFIX + "2.6.2"),
		ClusterContainer.from(TagNames.SKIPPER_2_7, SKIPPER_IMAGE_PREFIX + "2.7.4"),
		ClusterContainer.from(TagNames.SKIPPER_2_8, SKIPPER_IMAGE_PREFIX + "2.8.6"),
		ClusterContainer.from(TagNames.SKIPPER_2_9, SKIPPER_IMAGE_PREFIX + "2.9.3"),
		ClusterContainer.from(TagNames.SKIPPER_2_11, SKIPPER_IMAGE_PREFIX + "2.11.2")
	);

	public final static List<ClusterContainer> DATABASE_CONTAINERS = Arrays.asList(
		ClusterContainer.from(TagNames.POSTGRES_10, "postgres:10", TagNames.POSTGRES),
		ClusterContainer.from(TagNames.POSTGRES_14, "postgres:14", TagNames.POSTGRES_14),
		ClusterContainer.from(TagNames.MARIADB_10_4, "mariadb:10.4", TagNames.MARIADB),
		ClusterContainer.from(TagNames.MARIADB_10_5, "mariadb:10.5", TagNames.MARIADB),
		ClusterContainer.from(TagNames.MARIADB_10_6, "mariadb:10.6", TagNames.MARIADB),
		ClusterContainer.from(TagNames.MSSQL_2019_CU10_ubuntu_20_04, "mcr.microsoft.com/mssql/server:2019-CU10-ubuntu-20.04", TagNames.MSSQL),
		ClusterContainer.from(TagNames.MSSQL_2019_CU16_ubuntu_20_04, "mcr.microsoft.com/mssql/server:2019-CU16-ubuntu-20.04", TagNames.MSSQL),
		ClusterContainer.from(TagNames.MSSQL_2022_CTP2_0_ubuntu_20_04, "mcr.microsoft.com/mssql/server:2022-CTP2.0-ubuntu-20.04", TagNames.MSSQL),
		ClusterContainer.from(TagNames.ORACLE_XE_18, "gvenzl/oracle-xe:18-slim-faststart", TagNames.ORACLE),
		ClusterContainer.from(TagNames.DB2_11_5_0_0a, "icr.io/db2_community/db2:11.5.0.0a", TagNames.DB2),
		ClusterContainer.from(TagNames.DB2_11_5_8_0, "icr.io/db2_community/db2:11.5.8.0", TagNames.DB2)
	);

	public final static List<ClusterContainer> OAUTH_CONTAINERS = Collections.singletonList(
		ClusterContainer.from(TagNames.UAA_4_32, "springcloud/scdf-uaa-test:4.32", TagNames.UAA)
	);

	@Autowired
	private IntegrationTestProperties testProperties;

	private GenericContainer<?> toolsContainer = null;

	@AfterEach
	public void cleanCluster() {
		if (dataflowCluster != null) {
			dataflowCluster.stop();
		}
		dataflowCluster = null;
		if (toolsContainer != null) {
			toolsContainer.stop();
			toolsContainer = null;
		}
	}

	@BeforeEach
	public void setupCluster() {
		this.dataflowCluster = new DataflowCluster(getDatabaseContainers(), getOauthContainers(),
			getSkipperContainers(), getDataflowContainers(), testProperties.getDatabase().isSharedDatabase());
	}

	protected Container.ExecResult execInToolsContainer(String... command)
		throws UnsupportedOperationException, IOException, InterruptedException {
		if (toolsContainer == null) {
			toolsContainer = new GenericContainer<>("praqma/network-multitool:latest");
			toolsContainer.withNetwork(dataflowCluster.getNetwork());
			toolsContainer.start();
		}
		return toolsContainer.execInContainer(command);
	}

	protected DataflowCluster dataflowCluster;

	protected IntegrationTestProperties getTestProperties() {
		return testProperties;
	}

	protected String getDataflowLatestVersion() {
		return this.testProperties.getDatabase().getDataflowVersion();
	}

	protected String getSkipperLatestVersion() {
		return this.testProperties.getDatabase().getSkipperVersion();
	}

	protected List<ClusterContainer> getOauthContainers() {
		return new ArrayList<>(OAUTH_CONTAINERS);
	}

	protected List<ClusterContainer> getDatabaseContainers() {
		ArrayList<ClusterContainer> containers = new ArrayList<>(DATABASE_CONTAINERS);
		List<ClusterContainer> additional = this.testProperties.getDatabase().getAdditionalImages().getDatatabase()
			.entrySet().stream()
			.map(e -> ClusterContainer.from(e.getKey(), e.getValue().getImage(), e.getValue().getTag()))
			.collect(Collectors.toList());
		containers.addAll(additional);
		return containers;
	}

	protected List<ClusterContainer> getSkipperContainers() {
		ArrayList<ClusterContainer> containers = new ArrayList<>(SKIPPER_CONTAINERS);
		containers.add(ClusterContainer.from(TagNames.SKIPPER_main, SKIPPER_IMAGE_PREFIX + getSkipperLatestVersion()));
		List<Integer> jdkTags = Arrays.asList(8, 11, 17);
		for(Integer jdk : jdkTags) {
			containers.add(ClusterContainer.from(TagNames.SKIPPER_main + "-jdk" + jdk, SKIPPER_IMAGE_PREFIX + getSkipperLatestVersion() + "-jdk" + jdk));
		}
		return containers;
	}

	protected List<ClusterContainer> getDataflowContainers() {
		ArrayList<ClusterContainer> containers = new ArrayList<>(DATAFLOW_CONTAINERS);
		containers.add(ClusterContainer.from(TagNames.DATAFLOW_main, DATAFLOW_IMAGE_PREFIX + getDataflowLatestVersion()));
		List<Integer> jdkTags = Arrays.asList(8, 11, 17);
		for(Integer jdk : jdkTags) {
			containers.add(ClusterContainer.from(TagNames.DATAFLOW_main + "-jdk" + jdk, DATAFLOW_IMAGE_PREFIX + getDataflowLatestVersion() + "-jdk" + jdk));
		}
		return containers;
	}

	protected static void assertSkipperServerRunning(DataflowCluster dataflowCluster) {
		AssertUtils.assertSkipperServerRunning(dataflowCluster.getSkipperUrl());
	}

	protected static void assertSkipperServerNotRunning(DataflowCluster dataflowCluster) {
		AssertUtils.assertSkipperServerNotRunning(dataflowCluster.getSkipperUrl());
	}

	protected static void assertDataflowServerRunning(DataflowCluster dataflowCluster) {
		AssertUtils.assertDataflowServerRunning(UriComponentsBuilder.fromHttpUrl(dataflowCluster.getDataflowUrl()).pathSegment("about").toUriString());
	}

	protected static void assertDataflowServerNotRunning(DataflowCluster dataflowCluster) {
		AssertUtils.assertDataflowServerNotRunning(UriComponentsBuilder.fromHttpUrl(dataflowCluster.getDataflowUrl()).pathSegment("about").toUriString());
	}
}
