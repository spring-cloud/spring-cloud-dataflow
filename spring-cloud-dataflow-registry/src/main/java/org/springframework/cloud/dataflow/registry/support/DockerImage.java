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
package org.springframework.cloud.dataflow.registry.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Docker image name parsing utility Code from https://github.com/vmware/admiral
 */
public class DockerImage {

	public static final String SECTION_SEPARATOR = "/";

	public static final String TAG_SEPARATOR = ":";

	public static final String DEFAULT_NAMESPACE = "library";

	public static final String DEFAULT_TAG = "latest";

	private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-z0-9_]+");

	private static final List<String> OFFICIAL_REGISTRY_LIST = Collections
			.unmodifiableList(Arrays.asList(
					"registry.hub.docker.com",
					"docker.io"));

	private String host;

	private String namespace;

	private String repository;

	private String tag;

	/**
	 * parse a full image name (myhost:300/namespace/repo:tag) into its components
	 *
	 * @param imageName
	 * @return
	 */
	public static DockerImage fromImageName(String imageName) {
		String[] parts = imageName.split(SECTION_SEPARATOR);
		switch (parts.length) {
		case 0:
			throw new IllegalArgumentException("Invalid image format: " + imageName);

		case 1:
			// only one section - it is the repository name with optional tag
			return fromParts(null, null, parts[0]);

		case 2:
			// since there are two sections the second one can be either a host or a namespace
			if (isValidNamespace(parts[0])) {
				return fromParts(null, parts[0], parts[1]);
			}
			else {
				return fromParts(parts[0], null, parts[1]);
			}

		default:
			// three or more sections present: host, namespace and repo. According to Docker
			// documentation, the most common case is to have two path components in the name of the
			// repository, however, it is possible to have a different number of path segments:
			// https://docs.docker.com/registry/spec/api/#overview
			// We are going to treat the extra path arguments as part of the namespace, e.g. the
			// repo name host:port/path/to/repo will have "host:port" for host, "path/to" for
			// namespace and "repo" for name.

			String host = parts[0];
			String repo = parts[parts.length - 1];
			String namespace = imageName.substring(host.length() + SECTION_SEPARATOR.length(),
					imageName.length() - repo.length() - SECTION_SEPARATOR.length());
			return fromParts(host, namespace, repo);

		}
	}

	public static DockerImage fromParts(String hostPart, String namespacePart, String repoAndTagPart) {
		String[] repoParts = repoAndTagPart.split(TAG_SEPARATOR);
		switch (repoParts.length) {
		case 1:
			// no tag
			return fromParts(hostPart, namespacePart, repoParts[0], DEFAULT_TAG);

		case 2:
			// with tag
			return fromParts(hostPart, namespacePart, repoParts[0], repoParts[1]);

		default:
			throw new IllegalArgumentException("Invalid repository and tag format: "
					+ repoAndTagPart);
		}
	}

	public static DockerImage fromParts(String hostPart, String namespacePart, String repo,
			String tag) {

		DockerImage dockerImage = new DockerImage();
		dockerImage.host = hostPart;
		dockerImage.namespace = namespacePart;
		dockerImage.repository = repo;
		dockerImage.tag = tag;

		return dockerImage;
	}

	/**
	 * When a image name part can be ambiguously either host or namespace, check which one it
	 * is based on a regex of valid characters for the namespace part
	 *
	 * @param namespaceCandidate
	 * @return
	 */
	public static boolean isValidNamespace(String namespaceCandidate) {
		return NAMESPACE_PATTERN.matcher(namespaceCandidate).matches();
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @return the repository
	 */
	public String getRepository() {
		return repository;
	}

	public String getNamespaceAndRepo() {
		if (namespace != null) {
			return namespace + SECTION_SEPARATOR + repository;
		}
		else if (isDockerHubImage()) {
			return DEFAULT_NAMESPACE + SECTION_SEPARATOR + repository;
		}

		return repository;
	}

	/**
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}

	public boolean isDockerHubImage() {
		return host == null || OFFICIAL_REGISTRY_LIST.contains(host);
	}

	/**
	 * Convert to a canonical single string representation
	 *
	 * @return E.g.:
	 * <p/>
	 * registry.hub.docker.com/library/alpine -> alpine
	 * <p/>
	 * registry.hub.docker.com/mongons/mongo -> mongons/mongo
	 * <p/>
	 * registry.local.corp/proj/image -> registry.local.corp/proj/image
	 */
	@Override
	public String toString() {
		StringBuilder imageName = new StringBuilder();

		if (!isDockerHubImage()) {
			imageName.append(host);
			imageName.append(SECTION_SEPARATOR);
		}

		// If namespace is null, do not set the default value 'library' as not all
		// V2 registry implementations support this convention
		imageName.append(getNamespaceAndRepo());

		if (tag != null) {
			imageName.append(TAG_SEPARATOR);
			imageName.append(tag);
		}

		return imageName.toString();
	}
}
