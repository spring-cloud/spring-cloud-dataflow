/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.container.registry;

import java.util.Arrays;

import org.springframework.util.Assert;

/**
 *  - https://docs.docker.com/engine/reference/commandline/tag/#extended-description
 *     An image name is made up of slash-separated name components, optionally prefixed by a registry hostname.
 *
 *     The hostname must comply with standard DNS rules, BUT MAY NOT CONTAIN UNDERSCORES. If a hostname is present,
 *     it may optionally be followed by a port number in the format :8080. If not present, the command uses Docker’s
 *     public registry located at registry-1.docker.io by default.
 *
 *     Name components may contain lowercase letters, digits and separators. A separator is defined as a period, one or
 *     two underscores, or one or more dashes. A name component may not start or end with a separator.
 *
 *     A tag name must be valid ASCII and may contain lowercase and uppercase letters, digits, underscores, periods
 *     and dashes. A tag name may not start with a period or a dash and may contain a maximum of 128 characters.
 *
 *  - https://github.com/docker/docker.github.io/blob/master/docker-hub/repos.md#creating-repositories
 *     The repository name needs to be unique in that namespace, can be two to 255 characters, and can only contain
 *     lowercase letters, numbers or - and _
 *
 * - https://docs.docker.com/registry/spec/api/#overview
 *   1. A repository name is broken up into path components. A component of a repository name must be at least one
 *      lowercase, alpha-numeric characters, optionally separated by periods, dashes or underscores. More strictly,
 *      it must match the regular expression [a-z0-9]+(?:[._-][a-z0-9]+)*.
 *   2. If a repository name has two or more path components, they must be separated by a forward slash (“/”).
 *   3. The total length of a repository name, including slashes, must be less than 256 characters.
 *
 * - Heuristic logic implemented by Docker to detect domain part in repository name:
 *      https://github.com/docker/distribution/blob/master/reference/normalize.go#L91
 *
 * @author Christian Tzolov
 */
public class ContainerImageParser {

	/**
	 * Registry host (with optional port) to be used if the repository' image name has not specified one.
	 */
	private final String defaultRegistryHost;

	/**
	 * Repository image tag to be used when not specified by the image name.
	 */
	private final String defaultTag;

	/**
	 * Default namespace to be used if no namespace path component is provided by the Image Name.
	 */
	private final String officialRepositoryNamespace;

	/**
	 * Common registry hosts defaults.
	 */
	private static final String LOCALHOST_DOMAIN = "localhost";
	private static final String LEGACY_DEFAULT_DOMAIN = "index.docker.io";
	private static final String PLAIN_DOCKER_IO_DOMAIN = "docker.io";

	/**
	 * Characters used to split the image name in parts.
	 */
	private static final String SLASH_SEPARATOR = "/";
	private static final String PORT_SEPARATOR = ":";
	private static final String PERIOD_SEPARATOR = ".";

	private static final String TAG_SEPARATOR = ":";
	private static final String DIGEST_SEPARATOR = "@";

	public ContainerImageParser() {
		this(ContainerRegistryProperties.DOCKER_HUB_HOST,
				ContainerRegistryProperties.DEFAULT_TAG,
				ContainerRegistryProperties.DEFAULT_OFFICIAL_REPO_NAMESPACE);
	}

	public ContainerImageParser(String defaultRegistryHost, String defaultTag, String officialRepoName) {
		this.defaultRegistryHost = defaultRegistryHost;
		this.defaultTag = defaultTag;
		this.officialRepositoryNamespace = officialRepoName;
	}

	/**
	 * @param imageName  = [registry-host[:port]/] (namespace-path-component/)+ repository-name[:tag|@digest]
	 * @return Returns {@link ContainerImage}
	 */
	public ContainerImage parse(String imageName) {
		ContainerImage containerImageName = new ContainerImage();

		String[] registryHostAndRemainderSplit = splitDockerRegistryHost(imageName);
		String registryHost = registryHostAndRemainderSplit[0];
		String remainder = registryHostAndRemainderSplit[1];

		// Registry Host
		String[] hostAndPortSplit = registryHost.split(PORT_SEPARATOR);
		Assert.isTrue(hostAndPortSplit.length > 0 && hostAndPortSplit.length <= 2,
				"Invalid registry host address: " + registryHost);
		containerImageName.setHostname(hostAndPortSplit[0]);

		if (hostAndPortSplit.length == 2) { // has a port section
			containerImageName.setPort(hostAndPortSplit[1]);
		}

		String[] pathComponents = remainder.split(SLASH_SEPARATOR);

		// Repository name and tag
		String repositoryNameAndTag = pathComponents[pathComponents.length - 1];

		if (repositoryNameAndTag.contains(DIGEST_SEPARATOR)) {
			String[] repositoryNameAndDigestSplit = repositoryNameAndTag.split(DIGEST_SEPARATOR);
			Assert.isTrue(repositoryNameAndDigestSplit.length > 0 && repositoryNameAndDigestSplit.length <= 2,
					"Invalid repository name: " + repositoryNameAndTag);
			containerImageName.setRepositoryName(repositoryNameAndDigestSplit[0]);
			containerImageName.setRepositoryDigest(repositoryNameAndDigestSplit[1]);
		} else {
			String[] repositoryNameAndTagSplit = repositoryNameAndTag.split(TAG_SEPARATOR);
			Assert.isTrue(repositoryNameAndTagSplit.length > 0 && repositoryNameAndTagSplit.length <= 2,
					"Invalid repository name: " + repositoryNameAndTag);
			containerImageName.setRepositoryName(repositoryNameAndTagSplit[0]);

			String repositoryTag = (repositoryNameAndTagSplit.length == 2) ? repositoryNameAndTagSplit[1] : this.defaultTag;
			containerImageName.setRepositoryTag(repositoryTag);
		}

		// Namespace components
		if (pathComponents.length >= 2) {
			String[] namespaceComponents = Arrays.copyOf(pathComponents, pathComponents.length - 1);
			containerImageName.setNamespaceComponents(namespaceComponents);
		}

		return containerImageName;
	}

	/**
	 * The Docker Image Name specification (https://github.com/docker/distribution/blob/master/reference/reference.go)
	 * don't provide deterministic rules to distinct registry host (e.g. domain) component from the rest of the
	 * repository name.
	 *
	 * Therefore here we re-implement the heuristic logic used by the Docker implementation itself:
	 * https://github.com/docker/distribution/blob/master/reference/normalize.go#L91
	 *
	 * @param imageName the full repository name (including the optional registry host and port).
	 * @return Returns two-element array, where the [0] part contains the registry host and the [1] part the repository
	 * name remainder.
	 */
	private String[] splitDockerRegistryHost(String imageName) {
		String registryHost;
		String remainder;
		int i = imageName.indexOf(SLASH_SEPARATOR);
		if ((i == -1) || (!(imageName.substring(0, i).contains(PERIOD_SEPARATOR) || imageName.substring(0, i).contains(PORT_SEPARATOR))
				&& !imageName.substring(0, i).equals(LOCALHOST_DOMAIN))) {
			// No registry host detected use the default host!
			registryHost = this.defaultRegistryHost;
			remainder = imageName;
		}
		else { // registry host detected
			registryHost = imageName.substring(0, i);
			remainder = imageName.substring(i + 1);
		}

		// Note that 'docker.io' has to be replaced by the actual registry host ''
		if (registryHost.equals(LEGACY_DEFAULT_DOMAIN) || registryHost.equals(PLAIN_DOCKER_IO_DOMAIN)) {
			registryHost = this.defaultRegistryHost;
		}

		if (registryHost.equals(this.defaultRegistryHost) && !remainder.contains(SLASH_SEPARATOR)) {
			remainder = this.officialRepositoryNamespace + SLASH_SEPARATOR + remainder;
		}

		return new String[] { registryHost, remainder };
	}
}
