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

import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public interface ReleaseRepositoryCustom {

	/**
	 * Find the latest in time, release object, by name.
	 * @param releaseName the name of the release
	 * @return the Release object
	 * @throws {@link ReleaseNotFoundException} if no Release for the given name can be found.
	 */
	@RestResource(exported = false)
	Release findLatestRelease(String releaseName);

	/**
	 * Find the latest in time, release object, by name and with the deployed status.
	 * @param releaseName the name of the release
	 * @return the Release object
	 * @throws {@link ReleaseNotFoundException} if no deployed Release for the given name can be found.
	 */
	@RestResource(exported = false)
	Release findLatestDeployedRelease(String releaseName);

	/**
	 * Find the latest in time, release object, by name whose status is neither unknown nor failed.
	 * This release can be used for upgrade/rollback operations.
	 * @param releaseName the name of the release
	 * @return the Release object
	 * @throws {@link ReleaseNotFoundException} if no latest Release (with the deployed/deleted status) for the given
	 * name can be found.
	 */
	@RestResource(exported = false)
	Release findLatestReleaseForUpdate(String releaseName);

	/**
	 * Find the release to rollback from the existing version.
	 *
	 * @param releaseName the name of the release to rollback
	 * @return the Release object to rollback to
	 * @throws {@link ReleaseNotFoundException} if no latest Release found to rollback to.
	 */
	@RestResource(exported = false)
	Release findReleaseToRollback(String releaseName);

	/**
	 * Find the release for the given release name and version
	 * @param releaseName the name of the release
	 * @param version the version of the release
	 * @return {@link ReleaseNotFoundException} if no Release for the given name and version
	 * can be found.
	 */
	@RestResource(exported = false)
	Release findByNameAndVersion(String releaseName, int version);

	/**
	 * Find the revisions of the release, by name.
	 * @param releaseName the name of the release
	 * @param revisions the maximum number of revisions of the release to look for
	 * @return the list of Releases with their revisions as history
	 * @throws SkipperException if no Release for the given name can be found.
	 */
	@RestResource(exported = false)
	List<Release> findReleaseRevisions(String releaseName, Integer revisions);

	/**
	 * Find the latest status (deployed or failed) of the release, by the name.
	 * @param releaseName the name is the wildcard expression
	 * @return list of releases (by the given name) that has the latest revision with the
	 * state either deployed or failed.
	 */
	@RestResource(exported = false)
	List<Release> findLatestDeployedOrFailed(String releaseName);

	/**
	 * Find the latest status (deployed or failed) of all the releases.
	 *
	 * @return list of releases that has the latest revision with the state either deployed or
	 * failed.
	 */
	@RestResource(exported = false)
	List<Release> findLatestDeployedOrFailed();

	/**
	 * Return the release by the given name if the most recent status of the release is
	 * {@link org.springframework.cloud.skipper.domain.StatusCode#DELETED}.
	 *
	 * @param releaseName the name of the release
	 * @return if the latest status of the release is deleted then the release is returned,
	 * otherwise null.
	 */
	@RestResource(exported = false)
	Release findLatestReleaseIfDeleted(String releaseName);

}
