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
package org.springframework.cloud.skipper.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * @author Mark Pollack
 */
@Component
public class PackageIndexDownloader implements ResourceLoaderAware {

	private final Logger logger = LoggerFactory.getLogger(PackageIndexDownloader.class);

	private ResourceLoader resourceLoader;

	private SkipperServerProperties skipperServerProperties;

	@Autowired
	public PackageIndexDownloader(SkipperServerProperties skipperServerProperties) {
		this.skipperServerProperties = skipperServerProperties;
	}

	public void downloadPackageIndexes() {
		for (Repository packageRepository : this.skipperServerProperties.getPackageRepositories()) {
			try {
				Resource resource = resourceLoader.getResource(packageRepository.getUrl()
						+ File.separator + "index.yml");
				logger.info("Downloading from " + resource);
				File packageDir = new File(skipperServerProperties.getPackageIndexDir());
				packageDir.mkdirs();
				File downloadedFile = new File(packageDir, computeFilename(resource));
				StreamUtils.copy(resource.getInputStream(), new FileOutputStream(downloadedFile));
			}
			catch (IOException e) {
				logger.error("Could not process package file from " + packageRepository, e);
			}

		}
	}

	public List<File> getIndexFiles() {
		List<File> files = new ArrayList<>();
		Path indexPath = Paths.get(skipperServerProperties.getPackageIndexDir());
		try (Stream<Path> paths = Files.walk(indexPath, 1)) {
			files = paths.filter(i -> i.toString().endsWith(".yml"))
					.map(i -> i.toAbsolutePath().toFile())
					.collect(Collectors.toList());
		}
		catch (IOException e) {
			logger.error("Could not read index files in path " + indexPath, e);
		}
		finally {
			return files;
		}
	}

	public String computeFilename(Resource resource) throws IOException {
		URI uri = resource.getURI();
		StringBuilder stringBuilder = new StringBuilder();
		String scheme = uri.getScheme();
		if (scheme.equals("file")) {
			stringBuilder.append("file");
			if (uri.getPath() != null) {
				stringBuilder.append(uri.getPath().replaceAll("/", "_"));
			}
			else {
				String relativeFilename = uri.getSchemeSpecificPart().replaceAll("^./", "/dot/");
				stringBuilder.append(relativeFilename.replaceAll("/", "_"));
			}
		}
		else if (scheme.equals("http") || scheme.equals("https")) {
			stringBuilder.append(uri.getHost()).append(uri.getPath().replaceAll("/", "_"));
		}
		else {
			logger.warn("Package repository with scheme " + scheme
					+ " is not supported.  Skipping processing this repository.");
		}
		return stringBuilder.toString();
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
