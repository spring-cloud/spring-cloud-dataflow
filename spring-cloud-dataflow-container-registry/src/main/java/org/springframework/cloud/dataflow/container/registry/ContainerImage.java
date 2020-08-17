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

import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Domain class to represent the Container Image Name components.
 *
 * An image name is made up of slash-separated name components, prefixed by a registry hostname and optional port number.
 * The name components define a namespace followed by a repository-name and a tag. The repository name needs to be
 * unique in that namespace.
 *
 *   The container image has following structure:
 *
 *   registry-hostname : port / repo-namespace / repo-name : tag|digest
 *   |     REGISTRY-HOST      |          REPOSITORY        | [TAG or DIGEST]|
 *
 *   - The repository namespace is made up of zero or more slash-separated path components (eg. '/ns1/ns2/.../nsN/').
 *   - The registry hostname (or IP) and the optional port parts together form the REGISTRY HOST. Later is used as a
 *     unique identifier of the Container Registry hosting this container image. If not explicitly specified, a default
 *     registry host value is used.
 *   - The repository namespace together with the repository name form an unique REPOSITORY identifier, unique  within
 *     the REGISTRY HOST.
 *   - The TAG or DIGEST can be used as image instance references. The TAG represents a particular REPOSITORY instance
 *     within the REGISTRY HOST. The DIGEST content-addressable identifier.
 *
 * @author Christian Tzolov
 */
public class ContainerImage {

	// https://dockr.ly/2T8cSH3
	private static final Pattern NAMESPACE_COMPONENT_PATTERN = Pattern.compile("[a-z0-9]+(?:[._\\-][a-z0-9]+)*");
	// The repository name needs to be unique in that namespace, can be two to 255 characters, and can only contain
	// lowercase letters, numbers or - and _ (https://bit.ly/2Vz8kev)
	private static final Pattern REPOSITORY_NAME_PATTERN = Pattern.compile("[a-z0-9\\-_]{2,255}");
	// A tag name must be valid ASCII and may contain lowercase and uppercase letters, digits, underscores, periods
	// and dashes. A tag name may not start with a period or a dash and may contain a maximum of 128 characters.
	// (https://dockr.ly/3chhQZF)
	private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9\\-_.]{0,127}$");
	// Digest format is defined inn the docker reference code (https://bit.ly/3l8HjYT) and (https://bit.ly/33nBcdk)
	private static final Pattern DIGEST_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9]*(?:[\\-_+.][A-Za-z][A-Za-z0-9]*)*[:][[\\p{XDigit}]]{32,}$");
	private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
	private static final Pattern IP_PATTERN = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");
	private static final Pattern PORT_PATTERN = Pattern.compile("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$");

	enum RepositoryReferenceType {tag, digest, unknown}

	/**
	 * Registry hostname or IP address where the image is stored.
	 */
	private String hostname;

	/**
	 * Optional registry port
	 */
	private String port;

	/**
	 * Optional namespace. Zero or more path components.
	 */
	private String repositoryNamespace;

	/**
	 * Repository name
	 */
	private String repositoryName;

	/**
	 * Repository tag
	 */
	private String repositoryTag;

	/**
	 * Repository digest
	 */
	private String repositoryDigest;

	/**
	 * Helper method that returns the full Registry host address (host:port)
	 */
	public String getRegistryHost() {
		return this.hostname + (StringUtils.hasText(this.port) ? ":" + this.port : "");
	}

	/**
	 * Helper method that returns the full Repository name (e.g. namespace/registryName) without the tag or digest.
	 */
	public String getRepository() {
		String ns = StringUtils.hasText(this.repositoryNamespace) ? this.repositoryNamespace + "/" : "";
		return ns + this.repositoryName;
	}

	/**
	 * @return hostname:port/repositoryNamespace/repositoryName:reference
	 */
	public String getCanonicalName() {
		return getRegistryHost() + "/" + getRepository() + getReferencePrefix() + getRepositoryReference();
	}

	// Validated getters and setters

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		Assert.isTrue(HOSTNAME_PATTERN.matcher(hostname).matches()
				|| IP_PATTERN.matcher(hostname).matches(), "Invalid registry hostname: " + hostname);
		this.hostname = hostname;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		Assert.isTrue(PORT_PATTERN.matcher(port).matches(), "Invalid registry port: " + port);
		this.port = port;
	}

	public String getRepositoryNamespace() {
		return repositoryNamespace;
	}

	public void setRepositoryNamespace(String repositoryNamespace) {
		this.repositoryNamespace = repositoryNamespace;
	}

	public void setNamespaceComponents(String[] namespaceComponents) {
		if (namespaceComponents != null && namespaceComponents.length > 0) {
			Stream.of(namespaceComponents).forEach(
					pathComponent -> Assert.isTrue(NAMESPACE_COMPONENT_PATTERN.matcher(pathComponent).matches(),
							"Invalid namespace path component: " + pathComponent));
			this.setRepositoryNamespace(String.join("/", namespaceComponents));
		}
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		Assert.isTrue(REPOSITORY_NAME_PATTERN.matcher(repositoryName).matches(),
				"Invalid repository name: " + repositoryName);
		this.repositoryName = repositoryName;
	}

	public String getRepositoryReference() {
		return (StringUtils.hasText(this.repositoryTag) ? repositoryTag : repositoryDigest);
	}

	private String getReferencePrefix() {
		if (getRepositoryReferenceType() == RepositoryReferenceType.digest) {
			return "@";
		}
		return ":";
	}

	public RepositoryReferenceType getRepositoryReferenceType() {
		if (StringUtils.hasText(this.repositoryTag)) {
			return RepositoryReferenceType.tag;
		} if (StringUtils.hasText(this.repositoryDigest)) {
			return RepositoryReferenceType.digest;
		}
		return RepositoryReferenceType.unknown;
	}

	public String getRepositoryTag() {
		return repositoryTag;
	}

	public void setRepositoryTag(String repositoryTag) {
		Assert.isTrue(TAG_PATTERN.matcher(repositoryTag).matches(), "Invalid repository tag: " + repositoryTag);
		Assert.isTrue(!StringUtils.hasText(this.repositoryDigest),
				"Can not set repository Tag because of existing Digest " + repositoryDigest);
		this.repositoryTag = repositoryTag;
	}

	public String getRepositoryDigest() {
		return repositoryDigest;
	}

	public void setRepositoryDigest(String repositoryDigest) {
		Assert.isTrue(DIGEST_PATTERN.matcher(repositoryDigest).matches(), "Invalid repository digest: " + repositoryDigest);
		Assert.isTrue(!StringUtils.hasText(this.repositoryTag),
				"Can not set repository digest because of existing tag " + repositoryTag);
		this.repositoryDigest = repositoryDigest;
	}

	@Override
	public String toString() {
		return "ContainerImage{ host='" + hostname + "', port='" + port + "', namespace='"
				+ repositoryNamespace + "', name='" + repositoryName
				+ (StringUtils.hasText(repositoryTag) ? "', tag='" + repositoryTag + "'}" : "', digest='" + repositoryDigest + "'}");
	}
}
