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

import org.springframework.cloud.skipper.domain.Release;

/**
 * @author Mark Pollack
 */
public interface ReleaseRepositoryCustom {

	/**
	 * Find the lasted in time, release object, by name.
	 * @param releaseName the name of the release
	 * @return the Release object
	 * @throws org.springframework.cloud.skipper.index.PackageException if no Release for the
	 * given name can be found.
	 */
	Release findLatestRelease(String releaseName);

	/**
	 * Find the release for the given release name and version
	 * @param releaseName the name of the release
	 * @param version the version of the release
	 * @return @throws org.springframework.cloud.skipper.index.PackageException if no Release
	 * for the given name and version can be found.
	 */
	Release findByNameAndVersion(String releaseName, int version);
}
