/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.cloud.skipper.server.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;
import org.springframework.cloud.skipper.server.repository.jpa.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.RepositoryRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

/**
 * Uses @Transactional for ease of re-using existing JPA managed objects within Spring's
 * managed test method transaction
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Chris Bono
 * @author Corneil du Plessis
 */
@ActiveProfiles("repo-test")
@Transactional
class PackageServiceTests extends AbstractIntegrationTest {

	private final Logger logger = LoggerFactory.getLogger(PackageServiceTests.class);

	@Autowired
	private PackageService packageService;

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Autowired
	private RepositoryRepository repositoryRepository;

	@Test
	void exceptions() {
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName("noname");
		packageMetadata.setVersion("noversion");
		assertThat(packageService).isNotNull();
		assertThatThrownBy(() -> packageService.downloadPackage(packageMetadata))
				.isInstanceOf(SkipperException.class)
				.withFailMessage(
						"Resource for Package name 'noname', version 'noversion' was not found in any repository.");
		assertThatThrownBy(() -> packageService.downloadPackage(packageMetadata)).isInstanceOf(SkipperException.class);
	}

	@Test
	void download() {
		PackageMetadata packageMetadata = packageMetadataRepository.findByNameAndVersionByMaxRepoOrder("log", "1.0.0");
		// Other tests may have caused the file to be loaded into the database, ensure we start
		// fresh.
		if (packageMetadata.getPackageFile() != null) {
			packageMetadata.setPackageFile(null);
			packageMetadataRepository.save(packageMetadata);
		}
		packageMetadata = packageMetadataRepository.findByNameAndVersionByMaxRepoOrder("log", "1.0.0");
		assertThat(packageMetadata).isNotNull();
		assertThat(packageMetadata.getPackageFile()).isNull();
		assertThat(packageService).isNotNull();
		assertThat(packageMetadata.getId()).isNotNull();
		assertThat(packageMetadata.getRepositoryId()).isNotNull();
		Repository repository = repositoryRepository.findById(packageMetadata.getRepositoryId()).orElse(null);
		assertThat(repository).isNotNull();

		Package downloadedPackage = packageService.downloadPackage(packageMetadata);
		assertThat(downloadedPackage.getMetadata().getPackageFile()).isNotNull();
		assertThat(downloadedPackage.getMetadata()).isEqualToIgnoringGivenFields(packageMetadata);
		assertThat(downloadedPackage.getTemplates()).isNotNull();
		assertThat(downloadedPackage.getConfigValues()).isNotNull();
		packageMetadata = packageMetadataRepository.findByNameAndVersionByMaxRepoOrder("log", "1.0.0");
		assertThat(packageMetadata.getPackageFile().getPackageBytes()).isNotNull();
	}

	@Test
	void upload() throws Exception {
		// Create throw away repository, treated to be a 'local' database repo by default for now.
		Repository repository = new Repository();
		repository.setName("database-repo");
		repository.setUrl("https://example.com/repository/");
		this.repositoryRepository.save(repository);

		// Package log 9.9.9 should not exist, since it hasn't been uploaded yet.
		PackageMetadata packageMetadata = packageMetadataRepository.findByNameAndVersionByMaxRepoOrder("log", "9.9.9");
		assertThat(packageMetadata).isNull();

		UploadRequest uploadProperties = new UploadRequest();
		uploadProperties.setRepoName("local");
		uploadProperties.setName("log");
		uploadProperties.setVersion("9.9.9");
		uploadProperties.setExtension("zip");
		Resource resource = new ClassPathResource("/org/springframework/cloud/skipper/server/service/log-9.9.9.zip");
		assertThat(resource.exists()).isTrue();
		byte[] originalPackageBytes = StreamUtils.copyToByteArray(resource.getInputStream());
		assertThat(originalPackageBytes).isNotEmpty();
		Assert.isTrue(originalPackageBytes.length != 0,
				"PackageServiceTests.Assert.isTrue: Package file as bytes must not be empty");
		uploadProperties.setPackageFileAsBytes(originalPackageBytes);

		// Upload new package
		assertThat(packageService).isNotNull();
		PackageMetadata uploadedPackageMetadata = this.packageService.upload(uploadProperties);
		assertThat(uploadedPackageMetadata.getName()).isEqualTo("log");
		assertThat(uploadedPackageMetadata.getVersion()).isEqualTo("9.9.9");
		assertThat(uploadedPackageMetadata.getId()).isNotNull();

		// Retrieve new package
		PackageMetadata retrievedPackageMetadata = packageMetadataRepository.findByNameAndVersionByMaxRepoOrder("log",
				"9.9.9");
		assertThat(retrievedPackageMetadata.getName()).isEqualTo("log");
		assertThat(retrievedPackageMetadata.getVersion()).isEqualTo("9.9.9");
		assertThat(retrievedPackageMetadata).isNotNull();
		assertThat(retrievedPackageMetadata.getPackageFile().getPackageBytes()).isNotNull();
		byte[] retrievedPackageBytes = retrievedPackageMetadata.getPackageFile().getPackageBytes();
		assertThat(originalPackageBytes).isEqualTo(retrievedPackageBytes);

		// Check that package can be deserialized from the database.
		Package downloadedPackage = packageService.downloadPackage(retrievedPackageMetadata);
		assertThat(downloadedPackage.getMetadata()).isEqualToIgnoringGivenFields(retrievedPackageMetadata);
		assertThat(downloadedPackage.getTemplates()).isNotNull();
		assertThat(downloadedPackage.getConfigValues()).isNotNull();

	}

	@Test
	void packageNameVersionMismatch() throws IOException {
		UploadRequest uploadRequest = new UploadRequest();
		uploadRequest.setRepoName("local");
		uploadRequest.setName("buggy");
		uploadRequest.setVersion("6.6.6");
		uploadRequest.setExtension("zip");
		Resource resource = new ClassPathResource("/org/springframework/cloud/skipper/server/service/buggy-6.6.6.zip");
		assertThat(resource.exists()).isTrue();
		byte[] originalPackageBytes = StreamUtils.copyToByteArray(resource.getInputStream());
		uploadRequest.setPackageFileAsBytes(originalPackageBytes);

		assertThat(originalPackageBytes).isNotEmpty();
		Assert.isTrue(originalPackageBytes.length != 0,
				"PackageServiceTests.Assert.isTrue: Package file as bytes must not be empty");

		try {
			this.packageService.upload(uploadRequest);
			fail("Expected exception to be thrown when upload request package name is different from " +
					"this inside the package.zip");
		}
		catch (SkipperException e) {
			assertThat(e.getMessage()).isEqualTo("Package definition in the request [buggy:6.6.6] differs from " +
					"one inside the package.yml [log:9.9.9]");
		}
	}

	@Test
	void invalidVersions() throws IOException {
		UploadRequest uploadRequest = new UploadRequest();
		uploadRequest.setRepoName("local");
		uploadRequest.setName("log");
		uploadRequest.setVersion("abc");
		uploadRequest.setExtension("zip");
		Resource resource = new ClassPathResource("/org/springframework/cloud/skipper/server/service/log-9.9.9.zip");
		assertThat(resource.exists()).isTrue();
		byte[] originalPackageBytes = StreamUtils.copyToByteArray(resource.getInputStream());
		assertThat(originalPackageBytes).isNotEmpty();
		Assert.isTrue(originalPackageBytes.length != 0,
				"PackageServiceTests.Assert.isTrue: Package file as bytes must not be empty");
		assertInvalidPackageVersion(uploadRequest);
		uploadRequest.setVersion("1abc");
		assertInvalidPackageVersion(uploadRequest);
		uploadRequest.setVersion("1.abc.2");
		assertInvalidPackageVersion(uploadRequest);
		uploadRequest.setVersion("a.b.c");
		assertInvalidPackageVersion(uploadRequest);
		uploadRequest.setVersion("a.b.c.2");
		assertInvalidPackageVersion(uploadRequest);
	}

	private void assertInvalidPackageVersion(UploadRequest uploadRequest) {
		try {
			PackageMetadata uploadedPackageMetadata = this.packageService.upload(uploadRequest);
			fail("Expected exception to be thrown when parsing invalid version = "
					+ uploadRequest.getVersion().trim());
		}
		catch (SkipperException e) {
			assertThat(e.getMessage()).contains("UploadRequest doesn't have a valid semantic version.  Version =");
		}
	}

	@Test
	void deserializePackage() {
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersionByMaxRepoOrder("log",
				"1.0.0");
		assertThat(packageService).isNotNull();
		Package pkg = packageService.downloadPackage(packageMetadata);
		assertThat(pkg).isNotNull();
		assertThat(pkg.getConfigValues().getRaw()).contains("1.2.0.RC1");
		assertThat(pkg.getMetadata()).isEqualToIgnoringGivenFields(packageMetadata, "id", "origin", "packageFile");
		assertThat(pkg.getTemplates()).hasSize(1);
		Template template = pkg.getTemplates().get(0);
		assertThat(template.getName()).isEqualTo("log.yml");
		assertThat(template.getData()).isNotEmpty();
	}

	@Test
	void deserializeNestedPackage() {
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersionByMaxRepoOrder("ticktock",
				"1.0.0");
		assertThat(packageService).isNotNull();
		Package pkg = packageService.downloadPackage(packageMetadata);
		assertThat(pkg).isNotNull();
		assertThat(pkg.getMetadata()).isEqualToIgnoringGivenFields(packageMetadata, "id", "origin", "packageFile");
		assertThat(pkg.getDependencies()).hasSize(2);
		List<String> packageNames = new ArrayList<>();
		packageNames.add(pkg.getDependencies().get(0).getMetadata().getName());
		packageNames.add(pkg.getDependencies().get(1).getMetadata().getName());
		assertThat(packageNames).containsExactlyInAnyOrder("time", "log");
		assertPackageContent(pkg.getDependencies().get(0));
		assertPackageContent(pkg.getDependencies().get(1));
	}

	private void assertPackageContent(Package pkgContent) {
		String packageName = pkgContent.getMetadata().getName();
		assertThat(packageName).isIn("time", "log");
		assertThat(pkgContent).isNotNull();
		assertConfigValues(pkgContent);
		if (packageName.equals("log")) {
			assertThat(pkgContent.getMetadata().getName()).isEqualTo("log");
			assertThat(pkgContent.getMetadata().getVersion()).isEqualTo("2.0.0");
		}
		else {
			assertThat(pkgContent.getMetadata().getName()).isEqualTo("time");
			assertThat(pkgContent.getMetadata().getVersion()).isEqualTo("2.0.0");
		}
	}

	@SuppressWarnings("unchecked")
	protected void assertConfigValues(Package pkg) {
		// Note same config values for both time and log
		ConfigValues configValues = pkg.getConfigValues();
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
		Map<String, Object> logConfigValueMap = (Map<String, Object>) yaml.load(configValues.getRaw());
		assertThat(logConfigValueMap).containsKeys("version", "spec");
		if (pkg.getMetadata().getName().equals("log")) {
			assertThat(logConfigValueMap).containsEntry("version", "1.1.0.RELEASE");
		}
		if (pkg.getMetadata().getName().equals("time")) {
			assertThat(logConfigValueMap).containsEntry("version", "1.2.0.RELEASE");
		}
		Map<String, Object> spec = (Map<String, Object>) logConfigValueMap.get("spec");
		assertThat(spec).hasSize(2);
		Map<String, String> applicationProperties = (Map<String, String>) spec.get("applicationProperties");
		assertThat(applicationProperties)
				.hasSize(1)
				.contains(entry("log.level", "DEBUG"));
		Map<String, String> deploymentProperties = (Map<String, String>) spec.get("deploymentProperties");
		assertThat(deploymentProperties)
				.hasSize(1)
				.contains(entry("memory", "1024m"));

	}
}
