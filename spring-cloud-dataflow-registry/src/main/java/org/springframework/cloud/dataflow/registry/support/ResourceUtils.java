/*
 * Copyright 2017 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.resource.support.DownloadingUrlResourceLoader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class ResourceUtils {

	/**
	 * Parse the version number from a {@link UrlResource}. It can match a simple
	 * {@code <artifactId>-<version>.jar} formatted name. For example, a resource ending in
	 * {@code file-sink-rabbit-1.2.0.RELEASE.jar} will return {@code 1.2.0.RELEASE}. Snapshot
	 * builds of the form {@code file-sink-rabbit-1.2.0.BUILD-SNAPSHOT.jar} and
	 * {@code file-sink-rabbit-1.2.0-SNAPSHOT.jar} are also supported
	 * @param urlResource
	 * @return
	 */
	public static String getUrlResourceVersion(UrlResource urlResource) {
		Matcher m = getMatcher(urlResource);
		return m.group(2) + m.group(3);
	}

	public static String getUrlResourceWithoutVersion(UrlResource urlResource) {
		String version = getUrlResourceVersion(urlResource);
		URI uri = getUri(urlResource);
		String theRest = uri.toString().substring(0, uri.toString().indexOf("-" + version));
		return theRest;
	}

	private static Matcher getMatcher(UrlResource urlResource) {
		String fileNameNoExtension = getFileNameNoExtension(urlResource);
		// Look for the last dash with a digit after it
		Pattern pattern = Pattern.compile("(.*)-(\\d)(.*?)");
		Matcher m = pattern.matcher(fileNameNoExtension);
		Assert.isTrue(m.matches(), "Could not parse version from " + getUri(urlResource)
				+ ", expected format is <artifactId>-<version>.jar");
		return m;
	}

	private static String getFileNameNoExtension(UrlResource urlResource) {
		URI uri = getUri(urlResource);
		String uriPath = uri.getPath();
		Assert.isTrue(StringUtils.hasText(uriPath), "URI path doesn't exist");
		String lastSegment = new File(uriPath).getName();
		Assert.isTrue(lastSegment.indexOf(".") != -1, "URI file name extension doesn't exist");
		return lastSegment.substring(0, lastSegment.lastIndexOf("."));
	}

	private static URI getUri(UrlResource urlResource) {
		URI uri;
		try {
			uri = urlResource.getURI();
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not get URI from " + urlResource.getDescription());
		}
		return uri;
	}

	/**
	 * Retrieve the corresponding {@link Resource} instance based on the URI String.
	 * Maven properties are used if the URI corresponds to maven resource.
	 *
	 * @param uriString String representation of the resource URI
	 * @param mavenProperties the maven properties to use in case of maven resource
	 * @return the resource instance
	 */
	public static Resource getResource(String uriString, MavenProperties mavenProperties) {
		Assert.isTrue(StringUtils.hasText(uriString), "Resource URI must not be empty");
		try {
			URI uri = new URI(uriString);
			String scheme = uri.getScheme();
			Assert.notNull(scheme, "a scheme (prefix) is required");
			if (scheme.equals("maven")) {
				String coordinates = uriString.replaceFirst("maven:\\/*", "");
				MavenResource mavenResource = MavenResource.parse(coordinates, mavenProperties);
				return mavenResource;
			}
			else if (scheme.equals("docker")) {
				String dockerUri = uriString.replaceFirst("docker:\\/*", "");
				return new DockerResource(dockerUri);
			}
			else {
				ResourceLoader resourceLoader = null;
				if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
					resourceLoader = new DefaultResourceLoader();
				}
				else {
					resourceLoader = new DownloadingUrlResourceLoader();
				}
				return resourceLoader.getResource(uriString);
			}
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Extracts the version from the resource. Supported resource types are {@link
	 * MavenResource}, {@link DockerResource}, and {@link UrlResource}. @param resource to be
	 * used. @return the version the resource. @throws
	 */
	public static String getResourceVersion(Resource resource) {
		Assert.notNull(resource, "resource must not be null");
		if (resource instanceof MavenResource) {
			MavenResource mavenResource = (MavenResource) resource;
			return mavenResource.getVersion();
		}
		else if (resource instanceof DockerResource) {
			DockerResource dockerResource = (DockerResource) resource;
			return getDockerImageTag(dockerResource);
		}
		else if (resource instanceof UrlResource) {
			return getUrlResourceVersion((UrlResource) resource);
		}
		else {
			throw new IllegalArgumentException("Do not support extracting resource from Resource of type "
					+ resource.getClass().getSimpleName());
		}
	}

	private static String getDockerImageTag(DockerResource dockerResource) {
		try {
			String uri = dockerResource.getURI().toString().substring("docker:".length());
			DockerImage dockerImage = DockerImage.fromImageName(uri);
			String tag = dockerImage.getTag();
			Assert.isTrue(StringUtils.hasText(tag), "Could not extract tag from " +
			dockerResource.getDescription());
			return tag;
		} catch (IOException e) {
				throw new IllegalArgumentException(
						"Docker Resource URI is not in expected format to extract version. " +
								dockerResource.getDescription(),
						e);
		}
	}

	/**
	 * Returns the version for the given resource URI string.
	 *
	 * @param uriString String representation of the resource URI
	 * @param mavenProperties the maven properties to use in case of maven resource
	 * @return the resource version
	 */
	public static String getResourceVersion(String uriString, MavenProperties mavenProperties) {
		return ResourceUtils.getResourceVersion(getResource(uriString, mavenProperties));
	}

	/**
	 * Extracts the string representing the resource with the version number extracted.
	 * @param resource to be used.
	 * @return String representation of the resource.
	 */
	public static String getResourceWithoutVersion(Resource resource) {
		Assert.notNull(resource, "resource must not be null");
		if (resource instanceof MavenResource) {
			MavenResource mavenResource = (MavenResource) resource;
			return String.format("maven://%s:%s",
					mavenResource.getGroupId(),
					mavenResource.getArtifactId());
		}
		else if (resource instanceof DockerResource) {
			DockerResource dockerResource = (DockerResource) resource;
			return getDockerImageWithoutVersion(dockerResource);
		}
		else if (resource instanceof UrlResource) {
			return getUrlResourceWithoutVersion((UrlResource) resource);
		}
		else {
			throw new IllegalArgumentException("Do not support extracting resource from Resource of type "
					+ resource.getClass().getSimpleName());
		}
	}

	private static String getDockerImageWithoutVersion(DockerResource dockerResource) {
		try {
			String uri = dockerResource.getURI().toString().substring("docker:".length());
			DockerImage dockerImage = DockerImage.fromImageName(uri);
			StringBuilder sb = new StringBuilder("docker:");
			if (StringUtils.hasText(dockerImage.getHost())) {
				sb.append(dockerImage.getHost());
				sb.append(DockerImage.SECTION_SEPARATOR);
			}
			sb.append(dockerImage.getNamespaceAndRepo());
			return sb.toString();
		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Docker Resource URI is not in expected format to extract version. " +
							dockerResource.getDescription(),
					e);
		}
	}


}
