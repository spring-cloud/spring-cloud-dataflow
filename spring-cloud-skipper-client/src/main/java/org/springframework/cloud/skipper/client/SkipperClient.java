/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.skipper.client;

import java.util.List;

import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.domain.CancelRequest;
import org.springframework.cloud.skipper.domain.CancelResponse;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.LogInfo;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.ScaleRequest;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.hateoas.CollectionModel;

/**
 * The main client side interface to communicate with the Skipper Server.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
public interface SkipperClient {

	static SkipperClient create(String baseUrl) {
		return new DefaultSkipperClient(baseUrl);
	}

	/**
	 * Return the template for deploying a Spring Boot Application using skipper.
	 * @return the SpringCloudDeployerApplication template
	 */
	Template getSpringCloudDeployerApplicationTemplate();

	/**
	 * @return The AboutInfo for the server
	 */
	AboutResource info();

	/**
	 * Search for package metadata.
	 * @param name optional name with wildcard support for searching
	 * @param details boolean flag to fetch all the metadata
	 * @return the package metadata with the projection set to summary
	 */
	CollectionModel<PackageMetadata> search(String name, boolean details);

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

	/**
	 * Upload the package.
	 *
	 * @param uploadRequest the properties for the package upload
	 * @return package metadata for the uploaded package
	 */
	PackageMetadata upload(UploadRequest uploadRequest);

	/**
	 * Delete a package
	 * @param packageName the name of the package
	 */
	void packageDelete(String packageName);

	/**
	 * Delete a specific release.
	 *  @param releaseName the release name
	 * @param deletePackage delete package when deleting the release
	 * */
	void delete(String releaseName, boolean deletePackage);

	/**
	 * Rollback a specific release.
	 *
	 * @param rollbackRequest the rollback request
	 * @return the rolled back {@link Release}
	 */
	Release rollback(RollbackRequest rollbackRequest);

	/**
	 * Rollback a specific release.
	 *
	 * @param releaseName the release name
	 * @param releaseVersion the release version
	 * @return the rolled back {@link Release}
	 * @see #rollback(RollbackRequest)
	 * @deprecated use rollback method taking a rollback request
	 */
	@Deprecated
	Release rollback(String releaseName, int releaseVersion);

	/**
	 * Sends a cancel request for current release operation
	 *
	 * @param cancelRequest the cancel request
	 * @return the cancel response
	 */
	CancelResponse cancel(CancelRequest cancelRequest);

	/**
	 * List the latest version of releases with status of deployed or failed.
	 *
	 * @param releaseNameLike the wildcard name of releases to search for
	 * @return the list of all matching releases
	 */
	List<Release> list(String releaseNameLike);

	/**
	 * List all releases for the given release name.
	 *
	 * @param releaseName the release name of the release to search for
	 * @return the list of all releases by the given name
	 */
	CollectionModel<Release> history(String releaseName);

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
	CollectionModel<Repository> listRepositories();

	/**
	 * List Platform Deployers
	 *
	 * @return the list of platforms deployers
	 */
	CollectionModel<Deployer> listDeployers();

	/**
	 * Return a status info of a last known release.
	 *
	 * @param releaseName the release name
	 * @return the status info of a release
	 */
	Info status(String releaseName);

	/**
	 * Return a status info of a release version.
	 *
	 * @param releaseName the release name
	 * @param releaseVersion the release version
	 * @return the status info of a release
	 */
	Info status(String releaseName, int releaseVersion);

	/**
	 * Return the manifest of the last known release. For packages with dependencies, the
	 * manifest includes the contents of those dependencies.
	 * @param releaseName the release name
	 * @return the manifest
	 */
	String manifest(String releaseName);

	/**
	 * Return a manifest info of a release version. For packages with dependencies, the
	 * manifest includes the contents of those dependencies.
	 *
	 * @param releaseName the release name
	 * @param releaseVersion the release version
	 * @return the manifest info of a release
	 */
	String manifest(String releaseName, int releaseVersion);


	/**
	 * Fetch the logs of the latest release identified by the given name.
	 *
	 * @param releaseName the release name
	 * @return the log content
	 */
	LogInfo getLog(String releaseName);


	/**
	 * Fetch the logs of the latest release identified by the given release name
	 * and a specific application name inside the release.
	 *
	 * @param releaseName the release name
	 * @param appName the application name
	 * @return the log content
	 */
	LogInfo getLog(String releaseName, String appName);

	/**
	 * Scale a release with a given scale request.
	 *
	 * @param releaseName the release name
	 * @param scaleRequest the scale request
	 * @return the status info of a release
	 */
	Release scale(String releaseName, ScaleRequest scaleRequest);
}
