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
package org.springframework.cloud.skipper.server.repository.jpa;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation for the {@link PackageMetadataRepositoryCustom} methods.
 *
 * @author Ilayaperumal Gopinathan
 */
public class PackageMetadataRepositoryImpl implements PackageMetadataRepositoryCustom {

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Autowired
	private RepositoryRepository repositoryRepository;

	@Override
	public PackageMetadata findByNameAndVersionByMaxRepoOrder(String packageName, String packageVersion) {
		List<PackageMetadata> packageMetadataList = this.packageMetadataRepository
				.findByNameAndVersionOrderByApiVersionDesc(packageName,
						packageVersion);
		if (packageMetadataList.size() == 0) {
			return null;
		}
		if (packageMetadataList.size() == 1) {
			return packageMetadataList.get(0);
		}
		List<Repository> repositoriesByRepoOrder = this.repositoryRepository.findAllByOrderByRepoOrderDesc();
		for (Repository repository : repositoriesByRepoOrder) {
			Long repoId = repository.getId();
			for (PackageMetadata packageMetadata : packageMetadataList) {
				if ((packageMetadata.getRepositoryId() != null) && packageMetadata.getRepositoryId().equals(repoId)) {
					return packageMetadata;
				}
			}
		}
		// if no repoId matches, then return the first package that matches (which has the highest
		// api version set).
		return packageMetadataList.get(0);
	}

	@Override
	public List<PackageMetadata> findByNameRequired(String packageName) {
		List<PackageMetadata> packageMetadata = this.packageMetadataRepository.findByName(packageName);
		if (packageMetadata.isEmpty()) {
			throw new SkipperException(String.format("Can not find a package named '%s'", packageName));
		}
		return packageMetadata;
	}

	@Override
	public PackageMetadata findByNameAndOptionalVersionRequired(String packageName, String packageVersion) {
		Assert.isTrue(StringUtils.hasText(packageName), "Package name must not be empty");
		PackageMetadata packageMetadata;
		if (StringUtils.hasText(packageVersion)) {
			packageMetadata = this.packageMetadataRepository.findByNameAndVersionByMaxRepoOrder(packageName,
					packageVersion);
		}
		else {
			packageMetadata = this.packageMetadataRepository.findFirstByNameOrderByVersionDesc(packageName);
		}
		if (packageMetadata == null) {
			throw new SkipperException(String.format("Can not find package '%s', version '%s'", packageName,
					packageVersion));
		}
		return packageMetadata;
	}
}
