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
package org.springframework.cloud.dataflow.registry.support;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Pollack
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
		Assert.isTrue(!theRest.contains(version), "URL resource with version as part of its path is not supported.");
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
			return formatDockerResource(dockerResource, (s, i) -> s.substring(i + 1, s.length()));
		}
		else if (resource instanceof UrlResource) {
			return getUrlResourceVersion((UrlResource) resource);
		}
		else {
			throw new IllegalArgumentException("Do not support extracting resource from Resource of type "
					+ resource.getClass().getSimpleName());
		}
	}

	private static String formatDockerResource(DockerResource dockerResource,
			BiFunction<String, Integer, String> function) {
		try {
			String dockerResourceUri = dockerResource.getURI().toString();
			Assert.isTrue(StringUtils.countOccurrencesOf(dockerResourceUri, ":") == 2,
					"Invalid docker resource URI: " + dockerResourceUri);
			int indexOfVersionSeparator = dockerResourceUri.lastIndexOf(":");
			return function.apply(dockerResourceUri, indexOfVersionSeparator);
		}
		catch (IOException e) {
			throw new IllegalArgumentException(
					"Docker Resource URI is not in expected format to extract version. Resource = " +
							dockerResource.getDescription(),
					e);
		}
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
			return formatDockerResource(dockerResource, (s, i) -> s.substring(0, i));
		}
		else if (resource instanceof UrlResource) {
			return getUrlResourceWithoutVersion((UrlResource) resource);
		}
		else {
			throw new IllegalArgumentException("Do not support extracting resource from Resource of type "
					+ resource.getClass().getSimpleName());
		}
	}

}
