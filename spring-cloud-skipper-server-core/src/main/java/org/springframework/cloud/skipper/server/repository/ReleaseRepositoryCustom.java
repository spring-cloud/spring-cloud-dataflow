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
package org.springframework.cloud.skipper.server.repository;

import java.util.List;

import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Release;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public interface ReleaseRepositoryCustom {

	/**
	 * Find the lasted in time, release object, by name.
	 * @param releaseName the name of the release
	 * @return the Release object
	 * @throws {@link ReleaseNotFoundException} if no Release for the given name can be found.
	 */
	Release findLatestRelease(String releaseName);

	/**
	 * Find the release for the given release name and version
	 * @param releaseName the name of the release
	 * @param version the version of the release
	 * @return {@link ReleaseNotFoundException} if no Release for the given name and version
	 * can be found.
	 */
	Release findByNameAndVersion(String releaseName, int version);

	/**
	 * Find the revisions of the release, by name.
	 * @param releaseName the name of the release
	 * @param revisions the maximum number of revisions of the release to look for
	 * @return the list of Releases with their revisions as history
	 * @throws SkipperException if no Release for the given name can be found.
	 */
	List<Release> findReleaseRevisions(String releaseName, int revisions);

	/**
	 * Find the latest status (deployed or failed) of the release, by the name.
	 * @param releaseName the name is the wildcard expression
	 * @return list of releases (by the given name) that has the latest revision with the
	 * state either deployed or failed.
	 */
	List<Release> findLatestDeployedOrFailed(String releaseName);

	/**
	 * Find the latest status (deployed or failed) of all the releases.
	 *
	 * @return list of releases that has the latest revision with the state either deployed or
	 * failed.
	 */
	List<Release> findLatestDeployedOrFailed();

	/**
	 * Return the release by the given name if the most recent status of the release is
	 * {@link org.springframework.cloud.skipper.domain.StatusCode.DELETED}.
	 *
	 * @param releaseName the name of the release
	 * @return if the latest status of the release is deleted then the release is returned, otherwise null.
	 */
	Release findLatestReleaseIfDeleted(String releaseName);

}
