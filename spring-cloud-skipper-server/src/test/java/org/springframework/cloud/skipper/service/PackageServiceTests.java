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
package org.springframework.cloud.skipper.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import io.jsonwebtoken.lang.Assert;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractIntegrationTest;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.PackageUploadProperties;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.index.PackageException;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.repository.RepositoryRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "spring.cloud.skipper.server.synchonizeIndexOnContextRefresh=true" })
public class PackageServiceTests extends AbstractIntegrationTest {

	@Autowired
	private PackageService packageService;

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Autowired
	private SkipperServerProperties skipperServerProperties;

	@Autowired
	private RepositoryRepository repositoryRepository;

	@Before
	public void cleanupPackageDir() {
		File packageDirectory = new File(skipperServerProperties.getPackageDir());
		FileSystemUtils.deleteRecursively(new File(skipperServerProperties.getPackageDir()));
		assertThat(packageDirectory).doesNotExist();
	}

	@Test
	public void testExceptions() {
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName("noname");
		packageMetadata.setVersion("noversion");
		assertThatThrownBy(() -> packageService.downloadPackage(packageMetadata))
				.isInstanceOf(PackageException.class)
				.withFailMessage(
						"Resource for Package name 'noname', version 'noversion' was not found in any repository.");
		assertThatThrownBy(() -> packageService.downloadPackage(packageMetadata)).isInstanceOf(PackageException.class);
	}

	@Test
	public void download() {
		PackageMetadata packageMetadata = packageMetadataRepository.findByNameAndVersion("log", "1.0.0");
		assertThat(packageMetadata).isNotNull();
		Package downloadedPackage = packageService.downloadPackage(packageMetadata);
		assertThat(downloadedPackage.getMetadata()).isEqualToIgnoringGivenFields(packageMetadata,
				"id", "origin", "packageFile");
		assertThat(downloadedPackage.getTemplates()).isNotNull();
		assertThat(downloadedPackage.getConfigValues()).isNotNull();
	}

	@Test
	public void upload() throws IOException {
		Repository repository = new Repository();
		repository.setName("relative-path-repo");
		repository.setUrl("http://example.com/repository/");
		this.repositoryRepository.save(repository);
		PackageMetadata packageMetadata = packageMetadataRepository.findByNameAndVersion("log", "1.0.0");
		assertThat(packageMetadata).isNotNull();
		this.packageService.downloadPackage(packageMetadata);
		// Check for packageFile after downloading
		PackageMetadata updatedPackageMetadata = packageMetadataRepository.findByNameAndVersion("log", "1.0.0");
		Assert.notNull(updatedPackageMetadata.getPackageFile());
		PackageUploadProperties properties = new PackageUploadProperties();
		properties.setRepoName("relative-path-repo");
		properties.setName("log");
		properties.setVersion("1.0.0");
		properties.setExtension("zip");
		properties.setFileToUpload(updatedPackageMetadata.getPackageFile());
		PackageMetadata uploadedPackageMetadata = this.packageService.upload(properties);
		assertThat(uploadedPackageMetadata.getName().equals("log")).isTrue();
		assertThat(uploadedPackageMetadata.getVersion().equals("1.0.0")).isTrue();
		assertThat(packageMetadataRepository.findByNameAndVersion("log", "1.0.0")).isNotNull();
	}

	@Test
	public void deserializePackage() {
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersion("log", "1.0.0");
		Package pkg = packageService.downloadPackage(packageMetadata);
		assertThat(pkg).isNotNull();
		assertThat(pkg.getConfigValues().getRaw()).contains("1024m");
		assertThat(pkg.getMetadata()).isEqualToIgnoringGivenFields(packageMetadata, "id", "origin", "packageFile");
		assertThat(pkg.getTemplates()).hasSize(1);
		Template template = pkg.getTemplates().get(0);
		assertThat(template.getName()).isEqualTo("log.yml");
		assertThat(template.getData()).isNotEmpty();
	}

	@Test
	public void deserializeNestedPackage() {
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersion("ticktock", "1.0.0");
		Package pkg = packageService.downloadPackage(packageMetadata);
		assertThat(pkg).isNotNull();
		assertThat(pkg.getMetadata()).isEqualToIgnoringGivenFields(packageMetadata, "id", "origin", "packageFile");
		assertThat(pkg.getDependencies()).hasSize(2);

		Package logPkg = pkg.getDependencies().get(0);
		assertThat(logPkg).isNotNull();
		assertThat(logPkg.getMetadata().getName()).isEqualTo("log");
		assertThat(logPkg.getMetadata().getVersion()).isEqualTo("2.0.0");
		assertConfigValues(logPkg);

		Package timePkg = pkg.getDependencies().get(1);
		assertThat(timePkg).isNotNull();
		assertThat(timePkg.getMetadata().getName()).isEqualTo("time");
		assertThat(timePkg.getMetadata().getVersion()).isEqualTo("2.0.0");
		assertConfigValues(timePkg);
	}

	protected void assertConfigValues(Package pkg) {
		ConfigValues configValues = pkg.getConfigValues();
		Yaml yaml = new Yaml();
		Map logConfigValueMap = (Map) yaml.load(configValues.getRaw());
		assertThat(logConfigValueMap).containsKeys("appVersion", "deployment");
		Map deploymentMap = (Map) logConfigValueMap.get("deployment");
		assertThat(deploymentMap).contains(entry("count", 1));
	}
}
