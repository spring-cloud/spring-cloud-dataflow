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
package org.springframework.cloud.dataflow.server.db.migration;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ObjectUtils;

/**
 * Base implementation for a {@link SqlCommand} copying data from
 * {@code URI_REGISTRY} table into {@code app_registration} table.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractMigrateUriRegistrySqlCommand extends SqlCommand {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMigrateUriRegistrySqlCommand.class);

	@Override
	public void handle(JdbcTemplate jdbcTemplate, Connection connection) {
		boolean migrate = false;
		try {
			jdbcTemplate.execute("select 1 from URI_REGISTRY");
			migrate = true;
			logger.info("Detected URI_REGISTRY, migrating");
		} catch (Exception e) {
			logger.info("Didn't detected URI_REGISTRY, skipping migration from URI_REGISTRY to app_registration");
		}

		if (migrate) {
			updateAppRegistration(jdbcTemplate, createAppRegistrationMigrationData(jdbcTemplate));
			dropUriRegistryTable(jdbcTemplate);
		}
	}

	@Override
	public boolean canHandleInJdbcTemplate() {
		return true;
	}

	/**
	 * Update app registration. Actual database implementation of this method is
	 * required to insert data based on list of
	 * {@code AppRegistrationMigrationData}'s and keep {@code hibernate_sequence}
	 * up-to-date.
	 *
	 * @param jdbcTemplate the jdbc template
	 * @param data the data
	 */
	protected abstract void updateAppRegistration(JdbcTemplate jdbcTemplate, List<AppRegistrationMigrationData> data);

	/**
	 * Creates a migration data from URI_REGISTRY table to get inserted
	 * into app_registration table. We're working on a raw data level
	 * thus no use hibernate, spring repositories nor entity classes.
	 *
	 * @param jdbcTemplate the jdbc template
	 * @return the list
	 */
	protected List<AppRegistrationMigrationData> createAppRegistrationMigrationData(JdbcTemplate jdbcTemplate) {
		Map<String, AppRegistrationMigrationData> data = new HashMap<>();
		// to get version using same existing logic
		AppResourceCommon arc = new AppResourceCommon(new MavenProperties(), new DelegatingResourceLoader());
		// track that we only add default version for one by just giving it to first one
		// just to be on a safe side
		Collection<String> defaultVersions = new HashSet<>();

		// format for old URI_REGISTRY is:
		// +-------------------------------+-------------------------------------------------------------------------------------------------+
		// | NAME                          | URI                                                                                             |
		// +-------------------------------+-------------------------------------------------------------------------------------------------+
		// | processor.bridge              | maven://org.springframework.cloud.stream.app:bridge-processor-rabbit:2.0.1.RELEASE              |
		// | processor.bridge.metadata     | maven://org.springframework.cloud.stream.app:bridge-processor-rabbit:jar:metadata:2.0.1.RELEASE |
		//
		// this needs to go to a new app_registration table structure where
		// uri and metadata and other fields are within same record.
		// AppRegistrationMigrationData is base of that info which then
		// gets actually updates with updateAppRegistration method.

		jdbcTemplate.query("select NAME, URI from URI_REGISTRY", rs -> {
			AppRegistrationMigrationData armd;
			String name = rs.getString(1);
			String uri = rs.getString(2);
			// we assume i.e processor.bridge and processor.bridge.metadata belong together
			String[] split = name.split("\\.");
			if (split.length == 2 || split.length == 3) {
				String key = split[0] + split[1];
				armd = data.getOrDefault(key, new AppRegistrationMigrationData());
				if (!defaultVersions.contains(key)) {
					armd.setDefaultVersion(true);
					defaultVersions.add(key);
				}
				if (split.length == 2) {
					// we got *.* first
					armd.setName(split[1]);
					armd.setType(ApplicationType.valueOf(split[0]).ordinal());
					armd.setUri(uri);
					try {
						armd.setVersion(arc.getResourceVersion(arc.getResource(uri)));
					}
					catch (Exception e) {
						logger.warn("Skipping URI_REGISTRY item {} migration with URI {} due to lack of version number in the URI", name, uri, e);
					}
				}
				else {
					// we got *.*.metadata first
					armd.setName(split[1]);
					armd.setType(ApplicationType.valueOf(split[0]).ordinal());
					armd.setMetadataUri(uri);
				}
				// either *.* or *.*.metadata, put it to map, either initial
				// insert or update
				data.put(key, armd);
			}
		});
		// if we skipper version errors, filter those out
		return data.values().stream().filter(i -> !ObjectUtils.isEmpty(i.getVersion())).collect(Collectors.toList());
	}

	/**
	 * Drop uri registry table.
	 *
	 * @param jdbcTemplate the jdbc template
	 */
	protected void dropUriRegistryTable(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.execute("drop table URI_REGISTRY");
	}

	/**
	 * Raw migration data from uri registry into app registration.
	 */
	protected static class AppRegistrationMigrationData {

		private String name;
		private int type;
		private String version;
		private String uri;
		private String metadataUri;
		private boolean defaultVersion;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public String getMetadataUri() {
			return metadataUri;
		}

		public void setMetadataUri(String metadataUri) {
			this.metadataUri = metadataUri;
		}

		public boolean isDefaultVersion() {
			return defaultVersion;
		}

		public void setDefaultVersion(boolean defaultVersion) {
			this.defaultVersion = defaultVersion;
		}
	}
}
