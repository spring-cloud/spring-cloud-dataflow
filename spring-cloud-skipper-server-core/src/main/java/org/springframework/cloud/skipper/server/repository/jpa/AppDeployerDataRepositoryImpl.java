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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.util.StringUtils;

/**
 * @author Mark Pollack
 */
public class AppDeployerDataRepositoryImpl implements AppDeployerDataRepositoryCustom {

	@Autowired
	private AppDeployerDataRepository appDeployerDataRepository;

	@Override
	public AppDeployerData findByReleaseNameAndReleaseVersionRequired(String releaseName, Integer releaseVersion) {
		AppDeployerData appDeployerData = appDeployerDataRepository.findByReleaseNameAndReleaseVersion(releaseName,
				releaseVersion);
		if (appDeployerData == null) {
			List<AppDeployerData> appDeployerDataList = StreamSupport
					.stream(appDeployerDataRepository.findAll().spliterator(), false)
					.collect(Collectors.toList());
			String existingDeployerData = StringUtils.collectionToCommaDelimitedString(appDeployerDataList);
			throw new SkipperException(String.format("No AppDeployerData found for release '%s' version '%s'." +
					"AppDeployerData = %s",
					releaseName, releaseVersion, existingDeployerData));
		}
		return appDeployerData;
	}
}
