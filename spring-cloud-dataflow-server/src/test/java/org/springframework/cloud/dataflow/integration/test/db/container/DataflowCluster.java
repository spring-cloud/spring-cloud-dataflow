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

package org.springframework.cloud.dataflow.integration.test.db.container;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startable;

import org.springframework.cloud.dataflow.integration.test.tags.TagNames;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class DataflowCluster implements Startable {

	private final static Logger logger = LoggerFactory.getLogger(DataflowCluster.class);

	private final int DATAFLOW_PORT = 9393;

	private final int SKIPPER_PORT = 7577;

	private final int POSTGRES_PORT = 5432;

	private final int MSSQL_PORT = 1433;

	private final int MARIADB_PORT = 3306;

	private final int ORACLE_PORT = 1521;

	private final int DB2_PORT = 50000;

	private final int UAA_PORT = 8099;

	private final int KEYCLOAK_PORT = 8080;

	private final Map<String, ClusterContainer> dataflowImages;

	private final Map<String, ClusterContainer> skipperImages;

	private final Map<String, ClusterContainer> databaseImages;

	private final Map<String, ClusterContainer> oauthImages;

	private final boolean sharedDatabase;

	private JdbcDatabaseContainer<?> runningDatabase;

	private GenericContainer<?> runningOauth;

	private JdbcDatabaseContainer<?> runningSkipperDatabase;

	private JdbcDatabaseContainer<?> runningDataflowDatabase;

	private SkipperContainer<?> runningSkipper;

	private DataflowContainer<?> runningDataflow;

	private ClusterContainer skipperDatabaseClusterContainer;

	private ClusterContainer dataflowDatabaseClusterContainer;

	private final Network network;

	public DataflowCluster(
		List<ClusterContainer> databaseContainers, List<ClusterContainer> oauthContainers,
		List<ClusterContainer> skipperContainers, List<ClusterContainer> dataflowContainers,
		boolean sharedDatabase
	) {
		Assert.notNull(databaseContainers, "databaseContainers must be set");
		Assert.notNull(oauthContainers, "oauthContainers must be set");
		Assert.notNull(skipperContainers, "skipperContainers must be set");
		Assert.notNull(dataflowContainers, "dataflowContainers must be set");
		this.databaseImages = databaseContainers.stream().collect(Collectors.toMap(cc -> cc.id, cc -> cc));
		this.oauthImages = oauthContainers.stream().collect(Collectors.toMap(cc -> cc.id, cc -> cc));
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
		stopIdentityProvider();
		stopDatabase();
		this.network.close();
	}

	public Network getNetwork() {
		return network;
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

	public void startIdentityProvider(String id) {
		ClusterContainer clusterContainer = this.oauthImages.get(id);
		Assert.notNull(clusterContainer, String.format("Unknown oauth %s", id));

		GenericContainer<?> oauthContainer;
		if (id.startsWith(TagNames.KEYCLOAK)) {
			KeycloakContainer keycloakContainer = new KeycloakContainer(clusterContainer.image)
				.withRealmImportFiles("/dataflow-realm.json", "/dataflow-users-0.json")
				.withAdminUsername("admin")
				.withAdminPassword("admin")
				.withExposedPorts(KEYCLOAK_PORT, 9000);
			oauthContainer = keycloakContainer;
		} else {
			oauthContainer = new GenericContainer<>(clusterContainer.image);
			oauthContainer.withExposedPorts(UAA_PORT);
		}
		oauthContainer.withNetworkAliases("oauth");
		oauthContainer.withNetwork(network);
		oauthContainer.withLogConsumer(outputFrame -> ContainerUtils.output("oauth", outputFrame));
		oauthContainer.start();
		if(id.startsWith(TagNames.KEYCLOAK)) {
			String authServerUrl = ((KeycloakContainer) oauthContainer).getAuthServerUrl();
			logger.info("keycloak using authServerUrl=" + authServerUrl);
		}
		runningOauth = oauthContainer;
	}

	public void stopIdentityProvider() {
		if (runningOauth != null) {
			runningOauth.stop();
		}
	}

	private GenericContainer<?> getIdentityProviderContainer() {
		return runningOauth;
	}

	public void startSkipper(String id) {
		Assert.state(runningSkipper == null, "There's a running skipper");
		ClusterContainer clusterContainer = this.skipperImages.get(id);
		Assert.notNull(clusterContainer, String.format("Unknown skipper %s", id));
		String skipperDatabaseAlias = sharedDatabase ? "database" : "skipperdatabase";
		SkipperContainer<?> skipperContainer = buildSkipperContainer(clusterContainer, getSkipperDatabaseContainer(),
			getIdentityProviderContainer(), skipperDatabaseAlias);

		skipperContainer.start();
		skipperContainer.followOutput(ContainerUtils::outputSkipper);
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
		logger.info("replaceSkipperAndDataflow:start:{},{}", skipperId, dataflowId);
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
		logger.info("replaceSkipperAndDataflow:done:{},{}", skipperId, dataflowId);
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
			getDataflowDatabaseContainer(), getIdentityProviderContainer(), runningSkipper, dataflowDatabaseAlias,
			skipperDatabaseAlias);
		dataflowContainer.start();
		dataflowContainer.followOutput(ContainerUtils::outputDataFlow);
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
		return String.format("http://%s:%s", runningDataflow.getHost(),
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
			} else if (clusterContainer.tag.startsWith(TagNames.MARIADB)) {
				MariaDBContainer<?> databaseContainer = new MariaDBContainer<>(clusterContainer.image);
				databaseContainer.withExposedPorts(MARIADB_PORT);
				databaseContainer.withDatabaseName("dataflow");
				databaseContainer.withUsername("spring");
				databaseContainer.withPassword("spring");
				databaseContainer.withNetworkAliases(databaseAlias);
				databaseContainer.withNetwork(network);
				return databaseContainer;
			} else if (clusterContainer.tag.startsWith(TagNames.MSSQL)) {
				MSSQLServerContainer<?> databaseContainer = new MSSQLServerContainer<>(
					clusterContainer.image);
				databaseContainer.acceptLicense();
				databaseContainer.withExposedPorts(MSSQL_PORT);
				databaseContainer.withNetworkAliases(databaseAlias);
				databaseContainer.withNetwork(network);
				databaseContainer.withUrlParam("encrypt", "false");
				return databaseContainer;
			} else if (clusterContainer.tag.startsWith(TagNames.ORACLE)) {
				OracleContainer databaseContainer = new CustomOracleContainer(clusterContainer.image);
				databaseContainer.withExposedPorts(ORACLE_PORT);
				databaseContainer.withUsername("spring");
				databaseContainer.withPassword("spring");
				databaseContainer.withNetworkAliases(databaseAlias);
				databaseContainer.withNetwork(network);
				return databaseContainer;
			} else if (clusterContainer.tag.startsWith(TagNames.DB2)) {
				Db2Container databaseContainer = new Db2Container(clusterContainer.image);
				databaseContainer.acceptLicense();
				databaseContainer.withExposedPorts(DB2_PORT);
				databaseContainer.withUsername("spring");
				databaseContainer.withPassword("spring");
				databaseContainer.withDatabaseName("spring");
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

	private SkipperContainer<?> buildSkipperContainer(
		ClusterContainer clusterContainer,
		JdbcDatabaseContainer<?> databaseContainer, GenericContainer<?> oauthContainer,
		String skipperDatabaseAlias
	) {
		logger.info("Building skipper container for {}", clusterContainer);
		SkipperContainer<?> skipperContainer = new SkipperContainer<>(clusterContainer.image);
		skipperContainer.withExposedPorts(SKIPPER_PORT);
		if (databaseContainer != null) {
			skipperContainer.withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false");
			skipperContainer.withEnv("SPRING_DATASOURCE_USERNAME", databaseContainer.getUsername());
			skipperContainer.withEnv("SPRING_DATASOURCE_PASSWORD", databaseContainer.getPassword());
			skipperContainer.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", databaseContainer.getDriverClassName());
			if (skipperDatabaseClusterContainer.tag.startsWith(TagNames.POSTGRES)) {
				skipperContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:postgresql://%s:%d/dataflow", skipperDatabaseAlias, POSTGRES_PORT));
			} else if (skipperDatabaseClusterContainer.tag.startsWith(TagNames.MARIADB)) {
				skipperContainer.withEnv("SPRING_DATASOURCE_URL",
						String.format("jdbc:mariadb://%s:%d/dataflow", skipperDatabaseAlias, MARIADB_PORT))
					.withEnv("SPRING_JPA_DATABASE_PLATFORM", "org.hibernate.dialect.MariaDB106Dialect");
			} else if (skipperDatabaseClusterContainer.tag.startsWith(TagNames.MSSQL)) {
				skipperContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:sqlserver://%s:%d;encrypt=false", skipperDatabaseAlias, MSSQL_PORT));
				skipperContainer.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
			} else if (skipperDatabaseClusterContainer.tag.startsWith(TagNames.ORACLE)) {
				skipperContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:oracle:thin:spring/spring@%s:%d/ORCLPDB1", skipperDatabaseAlias, ORACLE_PORT));
			} else if (skipperDatabaseClusterContainer.tag.startsWith(TagNames.DB2)) {
				skipperContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:db2://%s:%d/spring", skipperDatabaseAlias, DB2_PORT));
			} else {
				skipperContainer.withEnv("SPRING_DATASOURCE_URL", databaseContainer.getJdbcUrl());
			}
		}

		if (oauthContainer != null) {
			if(oauthContainer.getDockerImageName().contains(TagNames.KEYCLOAK)) {
				configureSkipperForKeycloak(skipperContainer);
			} else {
				configureSkipperForUAA(skipperContainer);
			}
		}

		skipperContainer.withNetworkAliases("skipper");
		skipperContainer.withNetwork(network);
		return skipperContainer;
	}

	private static void configureSkipperForUAA(SkipperContainer<?> skipperContainer) {
		String oauthUrlPrefix = "http://oauth:8099/uaa";
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.uaa.map-oauth-scopes", "true");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_CREATE", "dataflow.create");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_DEPLOY", "dataflow.deploy");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_DESTROY", "dataflow.destroy");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_MANAGE", "dataflow.manage");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_MODIFY", "dataflow.modify");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_SCHEDULE", "dataflow.schedule");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_VIEW", "dataflow.view");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_REDIRECT_URI", "{baseUrl}/login/oauth2/code/{registrationId}");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_AUTHORIZATION_GRANT_TYPE", "authorization_code");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_CLIENT_ID", "dataflow");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_CLIENT_SECRET", "secret");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_SCOPE", "openid,dataflow.create,dataflow.deploy,dataflow.destroy,dataflow.manage,dataflow.modify,dataflow.schedule,dataflow.view");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_JWK_SET_URI", oauthUrlPrefix +  "/token_keys");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_TOKEN_URI", oauthUrlPrefix +  "/oauth/token");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_USER_INFO_URI", oauthUrlPrefix +  "/userinfo");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_USER_NAME_ATTRIBUTE", "user_name");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_AUTHORIZATION_URI", oauthUrlPrefix +  "/oauth/authorize");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_INTROSPECTION_URI", oauthUrlPrefix +  "/introspect");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_ID", "dataflow");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_SECRET", "secret");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_AUTHORIZATION_CHECK_TOKEN_ACCESS", "isAuthenticated()");
	}

	private static void configureSkipperForKeycloak(SkipperContainer<?> skipperContainer) {
		String oauthUrlPrefix = "http://oauth:8080";
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.keycloak.map-oauth-scopes", "false");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.keycloak.map-group-claims", "false");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_CREATE", "dataflow_create");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_DEPLOY", "dataflow_deploy");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_DESTROY", "dataflow_destroy");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_MANAGE", "dataflow_manage");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_MODIFY", "dataflow_modify");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_SCHEDULE", "dataflow_schedule");
		skipperContainer.withEnv("spring.cloud.skipper.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_VIEW", "dataflow_view");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_REDIRECTURI", "{baseUrl}/login/oauth2/code/{registrationId}");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_AUTHORIZATIONGRANTTYPE", "authorization_code");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENTID", "dataflow");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENTSECRET", "secret");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENTAUTHENTICATIONMETHOD", "post");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_SCOPE", "openid, roles");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_ISSUERURI", oauthUrlPrefix +  "/realms/dataflow");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_JWKSETURI", oauthUrlPrefix +  "/realms/dataflow/protocol/openid-connect/certs");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_TOKENURI", oauthUrlPrefix +  "/realms/dataflow/protocol/openid-connect/token");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_USERINFOURI", oauthUrlPrefix +  "/realms/dataflow/protocol/openid-connect/userinfo");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_AUTHORIZATIONURI", oauthUrlPrefix +  "/realms/dataflow/protocol/openid-connect/auth");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_USER_NAME_ATTRIBUTE", "user_name");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_INTROSPECTIONURI", oauthUrlPrefix + "/realms/dataflow/protocol/openid-connect/token/introspect");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENTID", "dataflow");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENTSECRET", "090RucamvekrMLyGHMr4lkHX9xhAlsqK");
		skipperContainer.withEnv("SPRING_SECURITY_OAUTH2_AUTHORIZATION_CHECKTOKENACCESS", "isAuthenticated()");
	}

	private DataflowContainer<?> buildDataflowContainer(
		ClusterContainer clusterContainer,
		JdbcDatabaseContainer<?> databaseContainer, GenericContainer<?> oauthContainer,
		SkipperContainer<?> skipperContainer, String dataflowDatabaseAlias, String skipperDatabaseAlias
	) {
		logger.info("Building dataflow container for {}", clusterContainer);
		DataflowContainer<?> dataflowContainer = new DataflowContainer<>(clusterContainer.image);
		dataflowContainer.withExposedPorts(DATAFLOW_PORT);
		if (databaseContainer != null) {
			dataflowContainer.withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false");
			dataflowContainer.withEnv("SPRING_DATASOURCE_USERNAME", databaseContainer.getUsername());
			dataflowContainer.withEnv("SPRING_DATASOURCE_PASSWORD", databaseContainer.getPassword());
			dataflowContainer.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", databaseContainer.getDriverClassName());
			if (dataflowDatabaseClusterContainer.tag.startsWith(TagNames.POSTGRES)) {
				dataflowContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:postgresql://%s:%s/dataflow", dataflowDatabaseAlias, POSTGRES_PORT));
			} else if (dataflowDatabaseClusterContainer.tag.startsWith(TagNames.MARIADB)) {
				dataflowContainer.withEnv("SPRING_DATASOURCE_URL",
						String.format("jdbc:mariadb://%s:%s/dataflow", dataflowDatabaseAlias, MARIADB_PORT))
					.withEnv("SPRING_JPA_DATABASE_PLATFORM", "org.hibernate.dialect.MariaDB106Dialect");
			} else if (dataflowDatabaseClusterContainer.tag.startsWith(TagNames.MSSQL)) {
				dataflowContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:sqlserver://%s:%s;encrypt=false", dataflowDatabaseAlias, MSSQL_PORT));
			} else if (dataflowDatabaseClusterContainer.tag.startsWith(TagNames.ORACLE)) {
				dataflowContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:oracle:thin:spring/spring@%s:%s/ORCLPDB1", dataflowDatabaseAlias, ORACLE_PORT));
			} else if (dataflowDatabaseClusterContainer.tag.startsWith(TagNames.DB2)) {
				dataflowContainer.withEnv("SPRING_DATASOURCE_URL",
					String.format("jdbc:db2://%s:%s/spring", dataflowDatabaseAlias, DB2_PORT));
			} else {
				dataflowContainer.withEnv("SPRING_DATASOURCE_URL", databaseContainer.getJdbcUrl());
			}
		}
		dataflowContainer.withEnv("SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI", String.format("http://skipper:%s/api", SKIPPER_PORT));

		if (oauthContainer != null) {
			if(oauthContainer.getDockerImageName().contains(TagNames.KEYCLOAK)) {
				configureDataflowForKeycloak(dataflowContainer);
			} else {
				configureDataflowForUAA(dataflowContainer);
			}
		}

		dataflowContainer.withNetworkAliases("dataflow");
		dataflowContainer.withNetwork(network);
		return dataflowContainer;
	}

	private void configureDataflowForKeycloak(DataflowContainer<?> dataflowContainer) {
		String oauthUrlPrefix = "http://oauth:" + KEYCLOAK_PORT;
		dataflowContainer.withEnv("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY", "DEBUG");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.keycloak.map-oauth-scopes", "false");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.keycloak.map-group-claims", "false");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_CREATE", "dataflow_create");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_DEPLOY", "dataflow_deploy");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_DESTROY", "dataflow_destroy");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_MANAGE", "dataflow_manage");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_MODIFY", "dataflow_modify");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_SCHEDULE", "dataflow_schedule");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.keycloak.role-mappings.ROLE_VIEW", "dataflow_view");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_REDIRECTURI", "{baseUrl}/login/oauth2/code/{registrationId}");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_AUTHORIZATIONGRANTTYPE", "authorization_code");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENTID", "dataflow");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENTSECRET", "090RucamvekrMLyGHMr4lkHX9xhAlsqK");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_SCOPE", "openid,roles");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_ISSUERURI", oauthUrlPrefix +  "/realms/dataflow");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_JWKSETURI", oauthUrlPrefix +  "/realms/dataflow/protocol/openid-connect/certs");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_TOKENURI", oauthUrlPrefix +  "/realms/dataflow/protocol/openid-connect/token");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_USERINFOURI", oauthUrlPrefix +  "/realms/dataflow/protocol/openid-connect/userinfo");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_AUTHORIZATIONURI", oauthUrlPrefix +  "/realms/dataflow/protocol/openid-connect/auth");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_USERNAMEATTRIBUTE", "user_name");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_INTROSPECTIONURI", oauthUrlPrefix + "/realms/dataflow/protocol/openid-connect/token/introspect");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENTID", "dataflow");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENTSECRET", "090RucamvekrMLyGHMr4lkHX9xhAlsqK");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_AUTHORIZATION_CHECKTOKENACCESS", "isAuthenticated()");
	}

	private void configureDataflowForUAA(DataflowContainer<?> dataflowContainer) {
		String oauthUrlPrefix = "http://oauth:" + UAA_PORT + "/uaa";
		dataflowContainer.withEnv("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY", "DEBUG");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.uaa.map-oauth-scopes", "true");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_CREATE", "dataflow.create");
		dataflowContainer.withEnv( "spring.cloud.dataflow.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_DEPLOY", "dataflow.deploy");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_DESTROY", "dataflow.destroy");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_MANAGE", "dataflow.manage");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_MODIFY", "dataflow.modify");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_SCHEDULE", "dataflow.schedule");
		dataflowContainer.withEnv("spring.cloud.dataflow.security.authorization.provider-role-mappings.uaa.role-mappings.ROLE_VIEW", "dataflow.view");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_REDIRECT_URI", "{baseUrl}/login/oauth2/code/{registrationId}");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_AUTHORIZATION_GRANT_TYPE", "authorization_code");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_CLIENT_ID", "dataflow");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_CLIENT_SECRET", "secret");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_UAA_SCOPE", "openid,dataflow.create,dataflow.deploy,dataflow.destroy,dataflow.manage,dataflow.modify,dataflow.schedule,dataflow.view");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_JWK_SET_URI", oauthUrlPrefix + "/token_keys");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_TOKEN_URI", oauthUrlPrefix + "/oauth/token");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_USER_INFO_URI", oauthUrlPrefix + "/userinfo");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_USER_NAME_ATTRIBUTE", "user_name");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_UAA_AUTHORIZATION_URI", oauthUrlPrefix + "/oauth/authorize");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_INTROSPECTION_URI", oauthUrlPrefix + "/introspect");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_ID", "dataflow");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_SECRET", "secret");
		dataflowContainer.withEnv("SPRING_SECURITY_OAUTH2_AUTHORIZATION_CHECK_TOKEN_ACCESS", "isAuthenticated()");
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
