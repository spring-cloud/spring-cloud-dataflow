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
package org.springframework.cloud.skipper.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractIntegrationTest;
import org.springframework.cloud.skipper.domain.PackageMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
public class PackageMetadataRepositoryTests extends AbstractIntegrationTest {

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Before
	public void before() {
		packageMetadataRepository.deleteAll();
	}

	@Test
	public void basicCrud() {
		PackageMetadataCreator.createTwoPackages(packageMetadataRepository);
		Iterable<PackageMetadata> packages = packageMetadataRepository.findAll();
		assertThat(packages).isNotEmpty();
		assertThat(packages).hasSize(2);
		List<PackageMetadata> packagesNamed1 = packageMetadataRepository.findByName("package1");
		assertThat(packagesNamed1).isNotEmpty();
		assertThat(packagesNamed1).hasSize(1);
		assertThat(packagesNamed1.get(0).getMaintainer()).isEqualTo("Alan Hale Jr.");
		List<PackageMetadata> packagesNamed2 = packageMetadataRepository.findByName("package2");
		assertThat(packagesNamed2).isNotEmpty();
		assertThat(packagesNamed2).hasSize(1);
		assertThat(packagesNamed2.get(0).getMaintainer()).isEqualTo("Bob Denver");

	}

}
