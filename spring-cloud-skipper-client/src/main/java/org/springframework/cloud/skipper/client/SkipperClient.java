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
package org.springframework.cloud.skipper.client;

import java.util.List;

import org.springframework.cloud.skipper.domain.AboutInfo;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.hateoas.Resources;

/**
 * The main client side interface to communicate with the Skipper Server.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public interface SkipperClient {

	static SkipperClient create(String baseUrl) {
		return new DefaultSkipperClient(baseUrl);
	}

	/**
	 * @return The AboutInfo for the server
	 */
	AboutInfo info();

	/**
	 * Search for package metadata.
	 * @param name optional name with wildcard support for searching
	 * @param details boolean flag to fetch all the metadata.
	 * @return the package metadata with the projection set to summary
	 */
	Resources<PackageMetadata> search(String name, boolean details);

	/**
	 * Install the package.
	 *
	 * @param packageId the package Id.
	 * @param installProperties the (@link InstallProperties)
	 * @return the deployed {@link Release}
	 */
	String install(String packageId, InstallProperties installProperties);

	/**
	 * Install the package
	 * @param installRequest the package install request
	 * @return the installed {@link Release}
	 */
	Release install(InstallRequest installRequest);

	/**
	 * Upgrade a release.
	 * @param upgradeRequest the request to upgrade the release
	 * @return the upgraded {@link Release}
	 */
	Release upgrade(UpgradeRequest upgradeRequest);

	/*
	 * Upload the package.
	 *
	 * @param uploadRequest the properties for the package upload.
	 * @return package metadata for the uploaded package
	 */
	PackageMetadata upload(UploadRequest uploadRequest);

	/**
	 * Delete a specific release.
	 *
	 * @param releaseName the release name
	 * @return the deleted {@link Release}
	 */
	Release delete(String releaseName);

	/**
	 * Rollback a specific release.
	 *
	 * @param releaseName the release name
	 * @param releaseVersion the release version.
	 * @return the rolled back {@link Release}
	 */
	Release rollback(String releaseName, int releaseVersion);

	/**
	 * List the latest version of releases with status of deployed or failed.
	 *
	 * @param releaseNameLike the wildcard name of releases to search for
	 * @return the list of all matching releases
	 */
	List<Release> list(String releaseNameLike);

	/**
	 * List the history of versions for a given release.
	 *
	 * @param releaseName the release name of the release to search for
	 * @param maxRevisions the maximum number of revisions to get
	 * @return the list of all releases by the given name and revisions max.
	 */
	List<Release> history(String releaseName, String maxRevisions);

	/**
	 * List all releases for the given release name.
	 *
	 * @param releaseName the release name of the release to search for
	 * @return the list of all releases by the given name.
	 */
	Resources<Release> history(String releaseName);

	/**
	 * Add a new Package Repository.
	 *
	 * @param name the name of the repository
	 * @param rootUrl the root URL for the package
	 * @param sourceUrl the source URL for the packages
	 * @return the newly added Repository
	 */
	Repository addRepository(String name, String rootUrl, String sourceUrl);

	/**
	 * Delete a Package Repository.
	 *
	 * @param name the name of the repository
	 */
	void deleteRepository(String name);

	/**
	 * List Package Repositories.
	 *
	 * @return the list of package repositories
	 */
	Resources<Repository> listRepositories();
}
