/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.skipper.server;

import java.io.File;
import java.io.IOException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.autoconfigure.ResourceLoadingAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.config.SkipperServerConfiguration;
import org.springframework.cloud.skipper.server.config.SkipperServerPlatformConfiguration;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Base class to implement integration tests using the root application configuration.
 * Does not use @Transactional annotation since the state is updated by multiple threads.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author Glenn Renfro
 */
@SpringBootTest(classes = AbstractIntegrationTest.TestConfig.class, properties = "spring.main.allow-bean-definition-overriding=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public abstract class AbstractIntegrationTest extends AbstractAssertReleaseDeployedTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@RegisterExtension
	public LogTestNameRule logTestName = new LogTestNameRule();

	@Autowired
	protected ReleaseRepository releaseRepository;

	@Autowired
	protected DataSource dataSource;

	@Autowired
	protected ReleaseService releaseService;

	@Autowired
	protected ReleaseManager releaseManager;

	@Autowired
	protected SkipperStateMachineService skipperStateMachineService;

	private File dbScriptFile;

	@BeforeEach
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

	@AfterEach
	public void restoreEmptySchema() {
		// Add a sleep for now to give the local deployer a chance to install the app. This
		// should go away once we introduce spring state machine.
		try {
			Thread.sleep(5000);
			for (Release release : releaseRepository.findAll()) {
				if (release.getInfo().getStatus().getStatusCode() != StatusCode.DELETED) {
					try {
						logger.info("After test clean up, deleting release " + release.getName());
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

	@Override
	protected boolean isDeployed(String releaseName, int releaseVersion) {
		try {
			logger.info("Checking status of release={} version={}", releaseName, releaseVersion);
			// retrieve status from underlying AppDeployer
			Release release = this.releaseManager
					.status(releaseRepository.findByNameAndVersion(releaseName, releaseVersion));
			Info info = release.getInfo();

			logger.info("Status = " + info.getStatus());
			return info.getStatus().getStatusCode().equals(StatusCode.DEPLOYED) &&
					allAppsDeployed(info.getStatus().getAppStatusList());
		}
		catch (Exception e) {
			logger.error("Exception getting status", e);
			return false;
		}
	}

	protected Release install(InstallRequest installRequest) throws InterruptedException {
		Release release = skipperStateMachineService.installRelease(installRequest);
		assertReleaseIsDeployedSuccessfully(release.getName(), release.getVersion());
		return release;
	}

	protected Release upgrade(UpgradeRequest upgradeRequest) throws InterruptedException {
		return upgrade(upgradeRequest, true);
	}

	protected Release upgrade(UpgradeRequest upgradeRequest, boolean doAssert) throws InterruptedException {
		Release release = skipperStateMachineService.upgradeRelease(upgradeRequest);
		if (doAssert) {
			assertReleaseIsDeployedSuccessfully(release.getName(), release.getVersion());
		}
		return release;
	}

	protected Release rollback(String releaseName, int releaseVersion) throws InterruptedException {
		Release release = skipperStateMachineService.rollbackRelease(new RollbackRequest(releaseName, releaseVersion));
		// Need to use the value of version passed back from calling rollback,
		// since 0 implies most recent deleted release
		assertReleaseIsDeployedSuccessfully(release.getName(), release.getVersion());
		return release;
	}

	protected Release delete(String releaseName) {
		return releaseService.delete(releaseName, false);
	}

	protected Release delete(String releaseName, boolean deleteReleasePackage) {
		logger.info("Deleting release {} with package {}", releaseName, deleteReleasePackage);
		return releaseService.delete(releaseName, deleteReleasePackage);
	}

	@Configuration
	@ImportAutoConfiguration(classes = { JacksonAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
			HibernateJpaAutoConfiguration.class, StateMachineJpaRepositoriesAutoConfiguration.class,
			SkipperServerPlatformConfiguration.class, ResourceLoadingAutoConfiguration.class,
			LocalDeployerAutoConfiguration.class})
	@Import(SkipperServerConfiguration.class)
	@EnableWebMvc
	static class TestConfig {
	}

}
