/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;

/**
 * Allow to extract files from URIs such as http/https, classpath or files and copy them into a preconfigured
 * local {@link #rootPath} folder.
 * The extractor supports the 'http:', 'https:', 'file:' and 'classpath:' schema prefixes.
 *
 * If the input resource URI doesn't start with 'schema-prefix:' it is assumed that the location is relative
 * local folder. In this case the file is not copied to the {@link #rootPath} folder and the URI is used as it is.
 *
 * For example the following snippet:
 *
 * <code>
 *  Path tempFolder = ...;
 *
 *  String[] extractedResourcesUris = new ResourceExtractor(tempFolder).extractAll(
 * 	  "https://raw.githubusercontent.com/spring-cloud/spring-cloud-dataflow/master/src/docker-compose/docker-compose.yml",
 * 	  "classpath:/docker-compose-prometheus.yml",
 * 	  "file:/Users/Dev/projects/scdf/spring-cloud-dataflow/src/docker-compose/docker-compose-postgres.yml",
 * 	  "file:../src/docker-compose/docker-compose-cf.yml",
 * 	  "docker-compose-debug-dataflow.yml");
 * </code>
 *
 * Would extract all files with explicit prefixes (e.g. https:, classpath: file:) and return list of the
 * new URI locations:
 *
 * <code>
 * [
 *   /tmp/test/yml/docker-compose.yml,             // Copied resource form HTTPS location.
 *   /tmp/test/yml/docker-compose-prometheus.yml,  // Copied resource classpath.
 *   /tmp/test/yml/docker-compose-postgres.yml,    // Copied resource form an absolute file location.
 *   /tmp/test/yml/docker-compose-cf.yml,          // Copied resource form a relative file location.
 *   docker-compose-debug-dataflow.yml             // Resource is left in place and the original URI is used.
 * ]
 * </code>
 *
 * Note that all resources but the docker-compose-debug-dataflow.yml are copied to the temp folder.
 *
 * @author Christian Tzolov
 */
public class ResourceExtractor {

	private final Logger logger = LoggerFactory.getLogger(ResourceExtractor.class);

	/**
	 * Temporal folder where all resources are extracted.
	 */
	private final Path rootPath;

	/**
	 * Spring utility for loading resources (e.. class path or file system resources)
	 */
	private final ResourceLoader resourceLoader;

	public ResourceExtractor(Path rootPath) {
		this.rootPath = rootPath;
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * From a list of input resource URIs, copies the resources content under the {@link #rootPath} folder
	 * using the file name provided with the input URIs!
	 *
	 * If ann input resource URI doesn't specify an explicit schema prefix (e.g. classpath:, http:, fie:)
	 * the resource is not extracted to the {@link #rootPath} folder folder but left in place. Also in that case
	 * the input resource URI returned as it is.
	 *
	 * @param resourceUris list of resource URIs to be extracted.
	 * @return Returns list of the new file URIs after the extraction. If an input URI didn't use
	 * and explicit URI schema prefix the input URI is returned unmodified.
	 */
	public String[] extract(String... resourceUris) {
		return Arrays.stream(resourceUris)
				.map(this::extract)
				.filter(Objects::nonNull)
				.collect(Collectors.toList())
				.toArray(new String[resourceUris.length]);
	}

	/**
	 * For an input resource URI, such as classpath:/, file:/ or http/https:/, copies the resource content
	 * into a new file under the {@link #rootPath} folder using the file name provided with the URI!
	 *
	 * If the input resource URI doesn't specify an explicit schema prefix (e.g. classpath:, http:, fie:)
	 * the resource is not extracted but left in place and the input resource URI is returned.
	 *
	 * @param resourceUri Resource URI to be extracted. If the URI doesn't specify an explicit schema prefix
	 *                    the resource is not modified (e.g. it is left in place).
	 * @return The new URI location of the copied resource. If the input resource URI doesn't use and
	 * explicit URI schema prefix (e.g. classpath:, http:, fie:) the input URI is returned unmodified.
	 */
	public String extract(String resourceUri) {
		if (!resourceUri.contains(":")) {
			return resourceUri;
		}

		try {
			Resource resource = this.resourceLoader.getResource(resourceUri);
			Path localResourcePath = Paths.get(this.rootPath.toString(), resource.getFilename());
			FileCopyUtils.copy(resource.getInputStream(),
					new FileOutputStream(localResourcePath.toFile()));
			return localResourcePath.toString();
		}
		catch (IOException e) {
			logger.error("Failed to extract:" + resourceUri, e);
		}
		return null;
	}
}
