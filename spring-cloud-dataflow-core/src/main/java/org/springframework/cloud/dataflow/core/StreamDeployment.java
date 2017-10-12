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

package org.springframework.cloud.dataflow.core;

/**
 * Represents Stream deployment model.
 *
 * @author Ilayaperumal Gopinathan
 */
public class StreamDeployment {

	/**
	 * The name of the stream under deployment.
	 */
	private final String streamName;

	/**
	 * The deployer used for the stream deployment. The deployer could be app deployer or Skipper.
	 */
	private final String deployerName;

	/**
	 * The package used during the deployment (mainly used by Skipper).
	 */
	private final String packageName;

	/**
	 * The release name for the deployment in Skipper.
	 */
	private final String releaseName;

	/**
	 * The package repository name used by Skipper for the package used in the deployment.
	 */
	private final String repoName;

	public StreamDeployment(String streamName, String deployerName, String packageName, String releaseName,
			String repoName) {
		this.streamName = streamName;
		this.deployerName = deployerName;
		this.packageName = packageName;
		this.releaseName = releaseName;
		this.repoName = repoName;
	}

	public String getStreamName() {
		return streamName;
	}

	public String getDeployerName() {
		return deployerName;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getReleaseName() {
		return releaseName;
	}

	public String getRepoName() {
		return repoName;
	}


}
