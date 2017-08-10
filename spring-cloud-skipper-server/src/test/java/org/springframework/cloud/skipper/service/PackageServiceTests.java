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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractIntegrationTest;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
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

	@Before
	public void cleanupPackageDir() {
		File packageDirectory = new File(skipperServerProperties.getPackageDir());
		FileSystemUtils.deleteRecursively(new File(skipperServerProperties.getPackageDir()));
		assertThat(packageDirectory).doesNotExist();
	}

	@Test
	public void download() {

		PackageMetadata packageMetadata = packageMetadataRepository.findByNameAndVersion("log", "1.0.0");
		assertThat(packageMetadata).isNotNull();
		packageService.downloadPackage(packageMetadata);
		File packageDirectory = packageService.calculatePackageDirectory(packageMetadata);
		assertThat(packageDirectory).exists().canRead().canWrite();
		File packageFile = packageService.calculatePackageZipFile(packageMetadata, packageDirectory);
		assertThat(packageFile).exists();
		File unzippedPackageDirectory = packageService.calculatePackageUnzippedDirectory(packageMetadata);
		assertThat(unzippedPackageDirectory).exists();
		List<File> files;
		try (Stream<Path> paths = Files.walk(Paths.get(unzippedPackageDirectory.toString()), 2)) {
			files = paths.map(i -> i.toAbsolutePath().toFile()).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not process files in path " + unzippedPackageDirectory.toString(),
					e);
		}
		assertThat(files).extracting("name")
				.contains("values.yml", "package.yml", "log.yml");
	}

	@Test
	public void deserializePackage() {
		PackageMetadata packageMetadata = packageMetadataRepository.findByNameAndVersion("log", "1.0.0");
		packageService.downloadPackage(packageMetadata);
		Package pkg = packageService.loadPackage(packageMetadata);
		assertThat(pkg).isNotNull();
		assertThat(pkg.getConfigValues().getRaw()).contains("1024m");
		assertThat(pkg.getMetadata()).isEqualTo(packageMetadata);
		assertThat(pkg.getTemplates()).hasSize(1);
		Template template = pkg.getTemplates().get(0);
		assertThat(template.getName()).isEqualTo("log.yml");
		assertThat(template.getData()).isNotEmpty();
	}
}
