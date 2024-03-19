/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.cloud.skipper.server.db.migration;

import java.util.Collections;
import javax.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.server.EnableSkipperServer;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.jpa.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides for testing some basic database schema and JPA tests to catch potential issues with specific databases early.
 *
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = AbstractSkipperSmokeTest.LocalTestSkipperServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "repo-test"})
@TestPropertySource(properties = {
	"spring.jpa.hibernate.ddl-auto=none",
	"logging.level.org.springframework.cloud=info",
	"logging.level.org.hibernate=debug"
})
@Testcontainers
public abstract class AbstractSkipperSmokeTest {
	private static final Logger logger = LoggerFactory.getLogger(AbstractSkipperSmokeTest.class);

	@Autowired
	AppDeployerDataRepository appDeployerDataRepository;

	@Autowired
	ReleaseRepository releaseRepository;


	@Autowired
	Environment environment;

	@Autowired
	EntityManagerFactory entityManagerFactory;


	@Test
	public void testStart() {
		logger.info("started:{}", getClass().getSimpleName());
		AppDeployerData deployerData = new AppDeployerData();
		deployerData.setDeploymentDataUsingMap(Collections.singletonMap("a", "b"));
		deployerData.setReleaseVersion(1);
		deployerData.setReleaseName("a");
		deployerData = appDeployerDataRepository.save(deployerData);
		assertThat(deployerData.getId()).isNotNull();
		assertThat(deployerData.getId()).isNotEqualTo(0);
		assertThat(deployerData.getDeploymentDataAsMap()).isNotEmpty();
		assertThat(deployerData.getDeploymentDataAsMap()).containsEntry("a", "b");

		Release release = createRelease();
		releaseRepository.save(release);
		String kind = ManifestUtils.resolveKind(release.getManifest().getData());
		assertThat(kind).isNotBlank();
		Release loaded = releaseRepository.findTopByNameOrderByVersionDesc(release.getName());
		String loadedKind = ManifestUtils.resolveKind(loaded.getManifest().getData());

		assertThat(loadedKind).isEqualTo(kind);

		logger.info("completed:{}", getClass().getSimpleName());
	}

	private static Release createRelease() {
		Info info = Info.createNewInfo("some info");
		Manifest manifest = new Manifest();
		manifest.setData("kind: Deployment\nmetadata:\n    name: abc\n");
		Release release = new Release();
		release.setName("abc");
		release.setPlatformName("default");
		release.setConfigValues(new ConfigValues());

		Package pkg = new Package();
		PackageMetadata packageMetadata1 = new PackageMetadata();
		packageMetadata1.setApiVersion("skipper.spring.io/v1");
		packageMetadata1.setKind("SpringCloudDeployerApplication");
		packageMetadata1.setRepositoryId(1L);
		packageMetadata1.setName("package1");
		packageMetadata1.setVersion("1.0.0");
		pkg.setMetadata(packageMetadata1);
		release.setPkg(pkg);
		release.setVersion(1);
		release.setInfo(info);
		release.setManifest(manifest);
		return release;
	}

	@SpringBootApplication(exclude = {CloudFoundryDeployerAutoConfiguration.class,
		LocalDeployerAutoConfiguration.class,
		KubernetesAutoConfiguration.class,
		SessionAutoConfiguration.class,
		CommonSecurityAutoConfiguration.class
	})
	@EnableSkipperServer
	public static class LocalTestSkipperServer {
		public static void main(String[] args) {
			SpringApplication.run(LocalTestSkipperServer.class, args);
		}
	}
}
