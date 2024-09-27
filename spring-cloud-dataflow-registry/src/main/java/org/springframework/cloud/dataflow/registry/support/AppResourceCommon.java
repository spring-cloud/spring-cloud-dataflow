/*
 * Copyright 2018-2019 the original author or authors.
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
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 */
public class AppResourceCommon {

	/**
	 * the maven properties to use in case of maven resource
	 */
	private MavenProperties mavenProperties;

	/**
	 * Delegated resource loader for resolving metadata from the metadata URI
	 */
	private ResourceLoader metadataResourceLoader;


	public AppResourceCommon(MavenProperties mavenProperties, ResourceLoader resourceLoader) {
		Assert.notNull(mavenProperties, "Non null Maven Properties are required!");
		this.mavenProperties = mavenProperties;
		this.metadataResourceLoader = resourceLoader;
	}

	/**
	 * Extracts the version from the resource. Supported resource types are
	 * MavenResource, {@link DockerResource}, and {@link UrlResource}.
	 * @param resource the resource to use.
	 * @return the version the resource.
	 */
	public String getResourceVersion(Resource resource) {
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

	private String getDockerImageTag(DockerResource dockerResource) {
		try {
			String uri = dockerResource.getURI().toString().substring("docker:".length());
			DockerImage dockerImage = DockerImage.fromImageName(uri);
			String tag = dockerImage.getTag();
			Assert.isTrue(StringUtils.hasText(tag), "Could not extract tag from " +
					dockerResource.getDescription());
			return tag;
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Docker Resource URI is not in expected format to extract version. " +
					dockerResource.getDescription(), e);
		}
	}

	/**
	 * Parse the version number from a {@link UrlResource}. It can match a simple
	 * {@code <artifactId>-<version>.jar} formatted name. For example, a resource ending in
	 * {@code file-sink-rabbit-5.0.0.jar} will return {@code 5.0.0}. Snapshot
	 * builds of the form {@code file-sink-rabbit-5.0.1-SNAPSHOT.jar} and
	 * {@code file-sink-rabbit-5.0.1-SNAPSHOT-metadata.jar} are also supported
	 * @param urlResource
	 * @return
	 */
	String getUrlResourceVersion(UrlResource urlResource) {
		Matcher m = getMatcher(urlResource);
		return m.group(2) + m.group(3);
	}

	private Matcher getMatcher(UrlResource urlResource) {
		String fileNameNoExtension = getFileNameNoExtension(urlResource);
		// Look for the last dash with a digit after it
		Pattern pattern = Pattern.compile("(.*)-(\\d)(.*?)(-metadata)?");
		Matcher m = pattern.matcher(fileNameNoExtension);
		Assert.isTrue(m.matches(), "Could not parse version from " + getUri(urlResource)
				+ ", expected format is <artifactId>-<version>.jar or  <artifactId>-<version>-metadata.jar");
		return m;
	}

	private String getFileNameNoExtension(UrlResource urlResource) {
		URI uri = getUri(urlResource);
		String uriPath = uri.getPath();
		Assert.isTrue(StringUtils.hasText(uriPath), "URI path doesn't exist");
		String lastSegment = new File(uriPath).getName();
		Assert.isTrue(lastSegment.indexOf(".") != -1, "URI file name extension doesn't exist");
		return lastSegment.substring(0, lastSegment.lastIndexOf("."));
	}

	private URI getUri(UrlResource urlResource) {
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
	 * @param resourceUri String representation of the resource URI
	 * @return the resource instance
	 */
	public Resource getResource(String resourceUri) {
		Assert.isTrue(StringUtils.hasText(resourceUri), "Resource URI must not be empty");
		Resource result = null;
		try {
			String scheme = new URI(resourceUri).getScheme();
			if (scheme == null) {
				throw new IllegalArgumentException("Invalid URI schema for resource: " + resourceUri
						+ " Expected URI schema prefix like file://, http:// or classpath:// but got none");
			}
			scheme = scheme.toLowerCase();
			Assert.notNull(scheme, "a scheme (prefix) is required");

			switch (scheme) {
			case "maven":
				String coordinates = resourceUri.replaceFirst("maven:\\/*", "");
				result = MavenResource.parse(coordinates, mavenProperties);
				break;
			case "docker":
				String dockerUri = resourceUri.replaceFirst("docker:\\/*", "");
				result = new DockerResource(dockerUri);
				break;
			case "http":
			case "https":
				result = new DownloadingUrlResourceLoader().getResource(resourceUri);
				break;
			default:
				result = new DefaultResourceLoader().getResource(resourceUri);
			}
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * Returns a string representing the resource with version subtracted
	 * @param resource to be represented as string.
	 * @return String representation of the resource.
	 */
	public String getResourceWithoutVersion(Resource resource) {
		Assert.notNull(resource, "resource must not be null");
		if (resource instanceof MavenResource) {
			MavenResource mavenResource = (MavenResource) resource;
			StringBuilder mavenResourceStringBuilder = new StringBuilder();
			mavenResourceStringBuilder.append(String.format("maven://%s:%s",
					mavenResource.getGroupId(),
					mavenResource.getArtifactId()));
			if (StringUtils.hasText(mavenResource.getExtension())) {
				mavenResourceStringBuilder.append(":" + mavenResource.getExtension());
			}
			else {
				mavenResourceStringBuilder.append(":jar");
			}
			if (StringUtils.hasText(mavenResource.getClassifier())) {
				mavenResourceStringBuilder.append(":" + mavenResource.getClassifier());
			}
			return mavenResourceStringBuilder.toString();
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

	String getUrlResourceWithoutVersion(UrlResource urlResource) {
		String version = getUrlResourceVersion(urlResource);
		URI uri = getUri(urlResource);
		String theRest = uri.toString().substring(0, uri.toString().indexOf("-" + version));
		return theRest;
	}

	private String getDockerImageWithoutVersion(DockerResource dockerResource) {
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
		}
		catch (IOException e) {
			throw new IllegalArgumentException(
					"Docker Resource URI is not in expected format to extract version. " +
							dockerResource.getDescription(),
					e);
		}
	}

	/**
	 * Resolves the metadata resource if provided or falls back to the apps resources otherwise. For Docker app
	 * resource types returns null
	 * @param appUri the App Resource URI to fall back to in case of missing metadata URI
	 * @param metadataUri Metadata resource URI
	 * @return If metadata URI is not empty returns the Metadata resource. For empty metadataUri returns the App Resource
	 * or null in case of Docker
	 */
	public Resource getMetadataResource(URI appUri, URI metadataUri) {
		if (metadataUri != null) {
			return this.metadataResourceLoader.getResource(metadataUri.toString());
		}
		else {
			// If the metadata URI is not set, only the archive type app resource can serve as the metadata resource
			return this.getResource(appUri.toString());
		}
	}

}
