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
package org.springframework.cloud.dataflow.server.service;

import java.util.Collection;

import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Release;

/**
 * Extends the core {@link StreamService} with support for operations provided by Skipper.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
public interface SkipperStreamService extends StreamService {

	/**
	 * Update the stream using the UpdateStreamRequest.
	 *
	 * @param streamName the name of the stream to update
	 * @param updateStreamRequest the UpdateStreamRequest to use during the update
	 */
	void updateStream(String streamName, UpdateStreamRequest updateStreamRequest);

	/**
	 * Rollback the stream to the previous or a specific version of the stream.
	 *
	 * @param streamName the name of the stream to rollback
	 * @param releaseVersion the version to rollback to (if not specified, rollback to the previous deleted/deployed
	 * release version of the stream.
	 */
	void rollbackStream(String streamName, int releaseVersion);

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
	 * Get stream's deployment history
	 * @param releaseName Stream release name
	 * @return List or Releases for this release name
	 */
	Collection<Release> history(String releaseName);

	/**
	 * @return list of supported deployment platforms
	 */
	Collection<Deployer> platformList();

}
