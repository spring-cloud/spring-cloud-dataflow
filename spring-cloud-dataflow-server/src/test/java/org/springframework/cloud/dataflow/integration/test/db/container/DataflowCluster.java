/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.db.container;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startable;

import org.springframework.cloud.dataflow.integration.test.tags.TagNames;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

public class DataflowCluster implements Startable {

	private final static Logger logger = LoggerFactory.getLogger(DataflowCluster.class);
	private final int DATAFLOW_PORT = 9393;
	private final int SKIPPER_PORT = 7577;
	private final int POSTGRES_PORT = 5432;
	private final int MYSQL_PORT = 3306;
	private final int MARIADB_PORT = 3306;
	private final Map<String, ClusterContainer> dataflowImages;
	private final Map<String, ClusterContainer> skipperImages;
	private final Map<String, ClusterContainer> databaseImages;
	private final boolean sharedDatabase;
	private JdbcDatabaseContainer<?> runningDatabase;
	private JdbcDatabaseContainer<?> runningSkipperDatabase;
	private JdbcDatabaseContainer<?> runningDataflowDatabase;
	private SkipperContainer<?> runningSkipper;
	private DataflowContainer<?> runningDataflow;
	private ClusterContainer skipperDatabaseClusterContainer;
	private ClusterContainer dataflowDatabaseClusterContainer;
	private Network network;

	public DataflowCluster(List<ClusterContainer> databaseContainers, List<ClusterContainer> skipperContainers,
			List<ClusterContainer> dataflowContainers, boolean sharedDatabase) {
		Assert.notNull(databaseContainers, "databaseContainers must be set");
		Assert.notNull(skipperContainers, "skipperContainers must be set");
		Assert.notNull(dataflowContainers, "dataflowContainers must be set");
		this.databaseImages = databaseContainers.stream().collect(Collectors.toMap(cc -> cc.id, cc -> cc));
		this.skipperImages = skipperContainers.stream().collect(Collectors.toMap(cc -> cc.id, cc -> cc));
		this.dataflowImages = dataflowContainers.stream().collect(Collectors.toMap(cc -> cc.id, cc -> cc));
		this.sharedDatabase = sharedDatabase;
		this.network = Network.newNetwork();
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
		stopSkipper();
		stopDataflow();
		stopDatabase();
		this.network.close();
	}

	public void startSkipperDatabase(String id) {
		String skipperDatabaseAlias = sharedDatabase ? "database" : "skipperdatabase";
		if (sharedDatabase) {
			ClusterContainer clusterContainer = this.databaseImages.get(id);
			if (runningDatabase == null) {
				Assert.notNull(clusterContainer, String.format("Unknown database %s", id));
				JdbcDatabaseContainer<?> databaseContainer = buildDatabaseContainer(clusterContainer,
						skipperDatabaseAlias);
				databaseContainer.start();
				runningDatabase = databaseContainer;
			}
			skipperDatabaseClusterContainer = clusterContainer;
		} else {
			Assert.state(runningSkipperDatabase == null, "There's a running skipper database");
			ClusterContainer clusterContainer = this.databaseImages.get(id);
			Assert.notNull(clusterContainer, String.format("Unknown database %s", id));
			JdbcDatabaseContainer<?> databaseContainer = buildDatabaseContainer(clusterContainer, skipperDatabaseAlias);
			databaseContainer.start();
			runningSkipperDatabase = databaseContainer;
			skipperDatabaseClusterContainer = clusterContainer;
		}
	}

	public void startDataflowDatabase(String id) {
		String dataflowDatabaseAlias = sharedDatabase ? "database" : "dataflowdatabase";
		if (sharedDatabase) {
			ClusterContainer clusterContainer = this.databaseImages.get(id);
			if (runningDatabase == null) {
				Assert.notNull(clusterContainer, String.format("Unknown database %s", id));
				JdbcDatabaseContainer<?> databaseContainer = buildDatabaseContainer(clusterContainer,
						dataflowDatabaseAlias);
				databaseContainer.start();
				runningDatabase = databaseContainer;
			}
			dataflowDatabaseClusterContainer = clusterContainer;
		} else {
			Assert.state(runningDataflowDatabase == null, "There's a running dataflow database");
			ClusterContainer clusterContainer = this.databaseImages.get(id);
			Assert.notNull(clusterContainer, String.format("Unknown database %s", id));
			JdbcDatabaseContainer<?> databaseContainer = buildDatabaseContainer(clusterContainer,
					dataflowDatabaseAlias);
			databaseContainer.start();
			runningDataflowDatabase = databaseContainer;
			dataflowDatabaseClusterContainer = clusterContainer;
		}
	}

	public void stopDatabase() {
		if (runningDatabase != null) {
			runningDatabase.stop();
		}
		if (runningSkipperDatabase != null) {
			runningSkipperDatabase.stop();
		}
		if (runningDataflowDatabase != null) {
			runningDataflowDatabase.stop();
		}
	}

	private JdbcDatabaseContainer<?> getSkipperDatabaseContainer() {
		return sharedDatabase ? runningDatabase : runningSkipperDatabase;
	}

	private JdbcDatabaseContainer<?> getDataflowDatabaseContainer() {
		return sharedDatabase ? runningDatabase : runningDataflowDatabase;
	}

	public void startSkipper(String id) {
		Assert.state(runningSkipper == null, "There's a running skipper");
		ClusterContainer clusterContainer = this.skipperImages.get(id);
		Assert.notNull(clusterContainer, String.format("Unknown skipper %s", id));
		String skipperDatabaseAlias = sharedDatabase ? "database" : "skipperdatabase";
		SkipperContainer<?> skipperContainer = buildSkipperContainer(clusterContainer, getSkipperDatabaseContainer(),
				skipperDatabaseAlias);
		skipperContainer.start();
		runningSkipper = skipperContainer;
	}

	public void stopSkipper() {
		if (runningSkipper != null) {
			runningSkipper.stop();
			runningSkipper = null;
		}
	}

	public void replaceSkipper(String id) {
		Assert.state(runningSkipper != null, "There's no running skipper");
		ClusterContainer clusterContainer = this.skipperImages.get(id);
		Assert.notNull(clusterContainer, String.format("Unknown skipper %s", id));
		stopSkipper();
		startSkipper(id);
	}

	public void replaceSkipperAndDataflow(String skipperId, String dataflowId) {
		Assert.state(runningSkipper != null, "There's no running skipper");
		Assert.state(runningDataflow != null, "There's no running dataflow");

		ClusterContainer skipperClusterContainer = this.skipperImages.get(skipperId);
		Assert.notNull(skipperClusterContainer, String.format("Unknown skipper %s", skipperId));
		ClusterContainer dataflowClusterContainer = this.dataflowImages.get(dataflowId);
		Assert.notNull(dataflowClusterContainer, String.format("Unknown dataflow %s", dataflowId));

		stopSkipper();
		stopDataflow();

		startSkipper(skipperId);
		startDataflow(dataflowId);
	}

	public String getSkipperUrl() {
		Assert.state(runningSkipper != null, "There's no running skipper");
		return String.format("http://%s:%s/api/about", runningSkipper.getHost(),
				runningSkipper.getMappedPort(SKIPPER_PORT));
	}

	public void startDataflow(String id) {
		Assert.state(runningDataflow == null, "There's a running dataflow");
		Assert.state(runningSkipper != null, "There's no running skipper");
		ClusterContainer clusterContainer = this.dataflowImages.get(id);
		Assert.notNull(clusterContainer, String.format("Unknown dataflow %s", id));
		String dataflowDatabaseAlias = sharedDatabase ? "database" : "dataflowdatabase";
		String skipperDatabaseAlias = sharedDatabase ? "database" : "skipperdatabase";
		DataflowContainer<?> dataflowContainer = buildDataflowContainer(clusterContainer,
				getDataflowDatabaseContainer(), runningSkipper, dataflowDatabaseAlias, skipperDatabaseAlias);
		dataflowContainer.start();
		runningDataflow = dataflowContainer;
	}

	public void stopDataflow() {
		if (runningDataflow != null) {
			runningDataflow.stop();
			runningDataflow = null;
		}
	}

	public void replaceDataflow(String id) {
		Assert.state(runningDataflow != null, "There's no running dataflow");
		ClusterContainer clusterContainer = this.dataflowImages.get(id);
		Assert.notNull(clusterContainer, String.format("Unknown dataflow %s", id));
		stopDataflow();
		startDataflow(id);
	}

	public String getDataflowUrl() {
		Assert.state(runningDataflow != null, "There's no running dataflow");
		return String.format("http://%s:%s/about", runningDataflow.getHost(),
				runningDataflow.getMappedPort(DATAFLOW_PORT));
	}

	private JdbcDatabaseContainer<?> buildDatabaseContainer(ClusterContainer clusterContainer, String databaseAlias) {
		if (StringUtils.hasText(clusterContainer.tag)) {
			if (clusterContainer.tag.startsWith(TagNames.POSTGRES)) {
				PostgreSQLContainer<?> databaseContainer = new PostgreSQLContainer<>(clusterContainer.image);
				databaseContainer.withExposedPorts(POSTGRES_PORT);
				databaseContainer.withDatabaseName("dataflow");
				databaseContainer.withUsername("spring");
				databaseContainer.withPassword("spring");
				databaseContainer.withNetworkAliases(databaseAlias);
				databaseContainer.withNetwork(network);
				return databaseContainer;
			} else if (clusterContainer.tag.startsWith(TagNames.MYSQL)) {
				MySQLMariaDriverContainer<?> databaseContainer = new MySQLMariaDriverContainer<>(
						clusterContainer.image);
				databaseContainer.withExposedPorts(MYSQL_PORT);
				databaseContainer.withDatabaseName("dataflow");
				databaseContainer.withUsername("spring");
				databaseContainer.withPassword("spring");
				databaseContainer.withNetworkAliases(databaseAlias);
				databaseContainer.withNetwork(network);
				return databaseContainer;
			} else if (clusterContainer.tag.startsWith(TagNames.MARIADB)) {
				MariaDBContainer<?> databaseContainer = new MariaDBContainer<>(clusterContainer.image);
				databaseContainer.withExposedPorts(MARIADB_PORT);
				databaseContainer.withDatabaseName("dataflow");
				databaseContainer.withUsername("spring");
				databaseContainer.withPassword("spring");
				databaseContainer.withNetworkAliases(databaseAlias);
				databaseContainer.withNetwork(network);
				return databaseContainer;
			}
		}
		throw new IllegalArgumentException(String.format("Can't handle database %s", clusterContainer.tag));
	}

	public String getSkipperDatabaseHostJdbcUrl() {
		JdbcDatabaseContainer<?> container = getSkipperDatabaseContainer();
		Assert.notNull(container, "Skipper database not running");
		return container.getJdbcUrl();
	}

	public String getDataflowDatabaseHostJdbcUrl() {
		JdbcDatabaseContainer<?> container = getDataflowDatabaseContainer();
		Assert.notNull(container, "Dataflow database not running");
		return container.getJdbcUrl();
	}

	private SkipperContainer<?> buildSkipperContainer(ClusterContainer clusterContainer,
			JdbcDatabaseContainer<?> databaseContainer, String skipperDatabaseAlias) {
		logger.info("Building skipper container for {}", clusterContainer);
		SkipperContainer<?> skipperContainer = new SkipperContainer<>(clusterContainer.image);
		skipperContainer.withExposedPorts(SKIPPER_PORT);
		if (ObjectUtils.nullSafeEquals(skipperDatabaseClusterContainer.tag, TagNames.POSTGRES)) {
			skipperContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:postgresql://%s:%s/dataflow", skipperDatabaseAlias, POSTGRES_PORT));
			skipperContainer.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.postgresql.Driver");
		} else if (ObjectUtils.nullSafeEquals(skipperDatabaseClusterContainer.tag, TagNames.MYSQL)) {
			skipperContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:mariadb://%s:%s/dataflow", skipperDatabaseAlias, MYSQL_PORT));
			skipperContainer.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.mariadb.jdbc.Driver");
		} else if (ObjectUtils.nullSafeEquals(skipperDatabaseClusterContainer.tag, TagNames.MARIADB)) {
			skipperContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:mariadb://%s:%s/dataflow", skipperDatabaseAlias, MARIADB_PORT));
			skipperContainer.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.mariadb.jdbc.Driver");
		}
		skipperContainer.withEnv("SPRING_DATASOURCE_USERNAME", "spring");
		skipperContainer.withEnv("SPRING_DATASOURCE_PASSWORD", "spring");
		skipperContainer.withLogConsumer(new Slf4jLogConsumer(logger));
		skipperContainer.withNetworkAliases("skipper");
		skipperContainer.withNetwork(network);
		return skipperContainer;
	}

	private DataflowContainer<?> buildDataflowContainer(ClusterContainer clusterContainer,
			JdbcDatabaseContainer<?> databaseContainer, SkipperContainer<?> skipperContainer,
			String dataflowDatabaseAlias, String skipperDatabaseAlias) {
		logger.info("Building dataflow container for {}", clusterContainer);
		DataflowContainer<?> dataflowContainer = new DataflowContainer<>(clusterContainer.image);
		dataflowContainer.withExposedPorts(DATAFLOW_PORT);
		if (ObjectUtils.nullSafeEquals(dataflowDatabaseClusterContainer.tag, TagNames.POSTGRES)) {
			dataflowContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:postgresql://%s:%s/dataflow", dataflowDatabaseAlias, POSTGRES_PORT));
			dataflowContainer.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.postgresql.Driver");
		} else if (ObjectUtils.nullSafeEquals(dataflowDatabaseClusterContainer.tag, TagNames.MYSQL)) {
			dataflowContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:mariadb://%s:%s/dataflow", dataflowDatabaseAlias, MYSQL_PORT));
			dataflowContainer.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.mariadb.jdbc.Driver");
		} else if (ObjectUtils.nullSafeEquals(dataflowDatabaseClusterContainer.tag, TagNames.MARIADB)) {
			dataflowContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:mariadb://%s:%s/dataflow", dataflowDatabaseAlias, MARIADB_PORT));
			dataflowContainer.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.mariadb.jdbc.Driver");
		}
		dataflowContainer.withEnv("SPRING_DATASOURCE_USERNAME", "spring");
		dataflowContainer.withEnv("SPRING_DATASOURCE_PASSWORD", "spring");
		dataflowContainer.withEnv("SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI",
				String.format("http://%s:%s/api", "skipper", SKIPPER_PORT));
		dataflowContainer.withLogConsumer(new Slf4jLogConsumer(logger));
		databaseContainer.withNetworkAliases("dataflow");
		dataflowContainer.withNetwork(network);
		return dataflowContainer;
	}

	public static class ClusterContainer {
		String id;
		String image;
		String tag;

		ClusterContainer(String id, String image, String tag) {
			this.id = id;
			this.image = image;
			this.tag = tag;
		}

		public static ClusterContainer from(String id, String image) {
			return from(id, image, null);
		}

		public static ClusterContainer from(String id, String image, String tag) {
			return new ClusterContainer(id, image, tag);
		}

		@Override
		public String toString() {
			return "ClusterContainer [id=" + id + ", image=" + image + ", tag=" + tag + "]";
		}
	}
}
