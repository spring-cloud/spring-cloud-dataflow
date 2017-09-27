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
package org.springframework.cloud.skipper.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.index.PackageException;
import org.springframework.cloud.skipper.io.TempFileUtils;
import org.springframework.cloud.skipper.repository.RepositoryRepository;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;

/**
 * Downloads package metadata from known repositories.
 * @author Mark Pollack
 */
@Component
public class PackageMetadataService implements ResourceLoaderAware {

	private final Logger logger = LoggerFactory.getLogger(PackageMetadataService.class);

	private final RepositoryRepository repositoryRepository;

	private ResourceLoader resourceLoader;

	@Autowired
	public PackageMetadataService(RepositoryRepository repositoryRepository) {
		this.repositoryRepository = repositoryRepository;
	}

	/**
	 * Download package metadata from all repositories.
	 * @return A list of package metadata, not yet persisted in the PackageMetadataRepository.
	 */
	public List<PackageMetadata> downloadPackageMetadata() {
		List<PackageMetadata> finalMetadataList = new ArrayList<>();
		Path targetPath = null;
		try {
			targetPath = TempFileUtils.createTempDirectory("skipperIndex");
			for (Repository packageRepository : this.repositoryRepository.findAll()) {
				try {
					Resource resource = resourceLoader.getResource(packageRepository.getUrl()
							+ File.separator + "index.yml");
					logger.info("Downloading from " + resource);
					File downloadedFile = new File(targetPath.toFile(), computeFilename(resource));
					StreamUtils.copy(resource.getInputStream(), new FileOutputStream(downloadedFile));
					List<File> downloadedFileAsList = new ArrayList<>();
					downloadedFileAsList.add(downloadedFile);
					List<PackageMetadata> downloadedPackageMetadata = deserializeFromIndexFiles(downloadedFileAsList);
					for (PackageMetadata packageMetadata : downloadedPackageMetadata) {
						packageMetadata.setOrigin(packageRepository.getId());
					}
					finalMetadataList.addAll(downloadedPackageMetadata);
				}
				catch (IOException e) {
					throw new PackageException("Could not process package file from " + packageRepository.getName(), e);
				}
			}
		}
		finally {
			if (targetPath != null && !FileSystemUtils.deleteRecursively(targetPath.toFile())) {
				logger.warn("Temporary directory can not be deleted: " + targetPath);
			}
		}
		return finalMetadataList;
	}

	protected List<PackageMetadata> deserializeFromIndexFiles(List<File> indexFiles) {
		List<PackageMetadata> packageMetadataList = new ArrayList<>();
		YAMLMapper yamlMapper = new YAMLMapper();
		for (File indexFile : indexFiles) {
			try {
				MappingIterator<PackageMetadata> it = yamlMapper.readerFor(PackageMetadata.class).readValues(indexFile);
				while (it.hasNextValue()) {
					PackageMetadata packageMetadata = it.next();
					packageMetadataList.add(packageMetadata);
				}
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Can't parse Release manifest YAML", e);
			}
		}
		return packageMetadataList;
	}

	// package protected for testing
	String computeFilename(Resource resource) throws IOException {
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
