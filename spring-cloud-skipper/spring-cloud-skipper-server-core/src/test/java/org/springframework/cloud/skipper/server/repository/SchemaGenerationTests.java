/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.repository;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;
import org.springframework.cloud.skipper.server.config.SkipperServerConfiguration;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@ActiveProfiles("repo-test")
@Transactional
public class SchemaGenerationTests extends AbstractIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SkipperServerConfiguration.class);

	@Autowired
	private LocalContainerEntityManagerFactoryBean fb;

	@Test
	public void generateSchemaDdlFiles() throws Exception {

		final PersistenceUnitInfo persistenceUnitInfo = fb.getPersistenceUnitInfo();
		final File tempDir = Files.createTempDirectory("skipper-sql-").toFile();
		final List<String> supportedHibernateDialects = new ArrayList<>();

		supportedHibernateDialects.add("H2");
		supportedHibernateDialects.add("MySQL5");
		supportedHibernateDialects.add("Oracle10g");
		supportedHibernateDialects.add("PostgreSQL94");
		supportedHibernateDialects.add("DB2");
		supportedHibernateDialects.add("SQLServer2012");

		logger.info(
				"\n\nGenerating DDL scripts for the following dialects:\n\n"
						+ supportedHibernateDialects.stream().map((db) -> db + "Dialect").collect(Collectors.joining("\n")) + "\n");

		for (String supportedHibernateDialect : supportedHibernateDialects) {
			generateDdlFiles(supportedHibernateDialect, tempDir, persistenceUnitInfo);
		}

		logger.info("\n\nYou can find the DDL scripts in directory: " + tempDir.getAbsolutePath() + "\n");

	}

	private void generateDdlFiles(String dialect, File tempDir, PersistenceUnitInfo persistenceUnitInfo) {
		logger.info("Generating DDL script for " + dialect);

		final MetadataSources metadata = new MetadataSources(
				new StandardServiceRegistryBuilder()
						.applySetting("hibernate.dialect", "org.hibernate.dialect." + dialect + "Dialect")
						.applySetting("hibernate.physical_naming_strategy", CamelCaseAbbreviatingFieldNamingStrategy.class.getName())
						.applySetting("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName())
						.build());

		for (String clazz : persistenceUnitInfo.getManagedClassNames()) {
			logger.info(clazz);
			metadata.addAnnotatedClassName(clazz);
		}

		final SchemaExport export;
		try {
			export = new SchemaExport();
			export.setDelimiter(";");
			export.setFormat(true);
			export.setOutputFile(new File(tempDir, "schema-" + dialect.toLowerCase() + ".sql").getAbsolutePath());
		}
		catch (HibernateException e) {
			throw new IllegalStateException(e);
		}
		EnumSet<TargetType> targetTypes = EnumSet.of(TargetType.SCRIPT);
		export.execute(targetTypes, SchemaExport.Action.BOTH, metadata.buildMetadata());
	}
}
