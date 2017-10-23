/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.server;

import java.io.File;
import java.io.IOException;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.config.SkipperServerConfiguration;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.cloud.skipper.server.AbstractIntegrationTest.TestConfig;

/**
 * Base class to implement integration tests using the root application configuration.
 * Does not use @Transactional annotation since the state is updated by multiple threads.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public abstract class AbstractIntegrationTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected ReleaseRepository releaseRepository;

	@Autowired
	protected ReleaseService releaseService;

	@Autowired
	protected DataSource dataSource;

	private File dbScriptFile;

	protected void sleep() {
		try {
			Thread.sleep(20000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Before
	public void beforeDumpSchema() {
		releaseRepository.deleteAll();
		try {
			dbScriptFile = File.createTempFile(this.getClass().getSimpleName() + "-", ".sql");
		}
		catch (IOException e) {
			logger.error("Can't create temp file for h2 schema", e);
		}
		new JdbcTemplate(dataSource).execute("SCRIPT NOPASSWORDS DROP TO '" + dbScriptFile.getPath() + "'");
		dbScriptFile.deleteOnExit();
	}

	@After
	public void restoreEmptySchema() {
		// Add a sleep for now to give the local deployer a chance to install the app. This
		// should go away once we introduce spring state machine.
		try {
			Thread.sleep(5000);
			for (Release release : releaseRepository.findAll()) {
				if (release.getInfo().getStatus().getStatusCode() != StatusCode.DELETED) {
					try {
						releaseService.delete(release.getName());
					}
					catch (Exception e) {
						logger.error(
								"Error cleaning up resource in integration test for Release {}-v{}. Status = {}.  Message = {}",
								release.getName(), release.getVersion(), release.getInfo().getStatus().getStatusCode(),
								e.getMessage());
					}
				}
			}
		}
		catch (InterruptedException e) {
			logger.error("Exception while cleaning up resources", e);
		}
		new JdbcTemplate(dataSource).execute("RUNSCRIPT FROM '" + dbScriptFile.getPath() + "'");

	}

	@Configuration
	@ImportAutoConfiguration(classes = { JacksonAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
			HibernateJpaAutoConfiguration.class })
	@Import(SkipperServerConfiguration.class)
	@EnableWebMvc
	static class TestConfig {
	}
}
