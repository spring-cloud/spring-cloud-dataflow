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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.YamlConfigurationFactory;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.index.PackageException;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * @author Mark Pollack
 */
@Service
public class PackageService implements ResourceLoaderAware {

	private final Logger logger = LoggerFactory.getLogger(PackageService.class);

	private ResourceLoader resourceLoader;

	private SkipperServerProperties skipperServerProperties;

	@Autowired
	public PackageService(SkipperServerProperties skipperServerProperties) {
		this.skipperServerProperties = skipperServerProperties;
	}

	public void downloadPackage(PackageMetadata packageMetadata) {
		String originUrl = packageMetadata.getOrigin();
		String sourceUrl = originUrl + "/" + packageMetadata.getName() + "/" +
				packageMetadata.getName() + "-" + packageMetadata.getVersion() + ".zip";
		Resource sourceResource = resourceLoader.getResource(sourceUrl);
		File targetPath = calculatePackageDirectory(packageMetadata);
		targetPath.mkdirs();
		File targetFile = calculatePackageZipFile(packageMetadata, targetPath);
		try {
			StreamUtils.copy(sourceResource.getInputStream(), new FileOutputStream(targetFile));
			logger.info("Downloaded package [" + packageMetadata.getName() + "-" + packageMetadata.getVersion()
					+ "] from " + originUrl);
		}
		catch (IOException e) {
			throw new PackageException("Could not copy " + sourceUrl + " to " + targetFile, e);
		}
		ZipUtil.unpack(targetFile, targetPath);
	}

	public Package loadPackage(PackageMetadata packageMetadata) {
		// TODO check if already downloaded.
		String resolvedPackagePath = calculatePackageUnzippedDirectory(packageMetadata).getPath();
		// Get all files under path
		List<File> files;
		try (Stream<Path> paths = Files.walk(Paths.get(resolvedPackagePath), 1)) {
			files = paths.map(i -> i.toAbsolutePath().toFile()).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new PackageException("Could not process files in path " + resolvedPackagePath, e);
		}

		Package pkg = new Package();

		// Iterate over all files and "deserialize" the package.
		for (File file : files) {
			// Package metadata
			if (file.getName().equalsIgnoreCase("package.yaml") || file.getName().equalsIgnoreCase("package.yml")) {
				pkg.setMetadata(loadPackageMetadata(file, packageMetadata.getOrigin()));
				continue;
			}
			if (file.getName().equalsIgnoreCase("values.yaml") || file.getName().equalsIgnoreCase("values.yml")) {
				pkg.setConfigValues(loadConfigValues(file));
				continue;
			}
			// The template/manifiest file
			String absFileName = file.getAbsoluteFile().toString();
			if (absFileName.endsWith("/templates")) {
				pkg.setTemplates(loadTemplates(file));
				continue;
			}
			// TODO: Deal with recursive packages

		}

		return pkg;
	}

	private PackageMetadata loadPackageMetadata(File file, String origin) {
		YamlConfigurationFactory<PackageMetadata> factory = new YamlConfigurationFactory<PackageMetadata>(
				PackageMetadata.class);
		factory.setResource(new FileSystemResource(file));
		PackageMetadata packageMetadata;
		try {
			packageMetadata = factory.getObject();
		}
		catch (Exception e) {
			throw new PackageException("Exception processing yaml file " + file.getName(), e);
		}
		packageMetadata.setOrigin(origin);
		return packageMetadata;
	}

	private ConfigValues loadConfigValues(File file) {
		ConfigValues configValues = new ConfigValues();
		try {
			configValues.setRaw(new String(Files.readAllBytes(file.toPath()), "UTF-8"));
		}
		catch (IOException e) {
			throw new PackageException("Could read values file " + file.getAbsoluteFile(), e);
		}

		return configValues;
	}

	private List<Template> loadTemplates(File templatePath) {
		List<File> files;
		try (Stream<Path> paths = Files.walk(Paths.get(templatePath.getAbsolutePath()), 1)) {
			files = paths.map(i -> i.toAbsolutePath().toFile()).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new PackageException("Could not process files in template path " + templatePath, e);
		}

		List<Template> templates = new ArrayList<>();
		for (File file : files) {
			if (isYamlFile(file)) {
				Template template = new Template();
				template.setName(file.getName());
				try {
					template.setData(new String(Files.readAllBytes(file.toPath()), "UTF-8"));
				}
				catch (IOException e) {
					throw new PackageException("Could read template file " + file.getAbsoluteFile(), e);
				}
				templates.add(template);
			}
		}
		return templates;
	}

	private boolean isYamlFile(File file) {
		Path path = Paths.get(file.getAbsolutePath());
		String fileName = path.getFileName().toString();
		if (!fileName.startsWith(".")) {
			return (fileName.endsWith("yml") || fileName.endsWith("yaml"));
		}
		return false;
	}

	protected File calculatePackageZipFile(PackageMetadata packageMetadata, File targetPath) {
		return new File(targetPath, packageMetadata.getName() + "-" + packageMetadata.getVersion() + ".zip");
	}

	public File calculatePackageUnzippedDirectory(PackageMetadata packageMetadata) {
		return new File(calculatePackageDirectory(packageMetadata),
				packageMetadata.getName() + "-" + packageMetadata.getVersion());
	}

	/**
	 * Give the PackageMetadata, return the directory where the package will be downloaded.
	 * The directory takes the server's PackageDir configuraiton property and appends the
	 * package name taken from the metadata.
	 * @param packageMetadata the package's metadata.
	 * @return The directory where the package will be downloaded.
	 */
	public File calculatePackageDirectory(PackageMetadata packageMetadata) {
		return new File(skipperServerProperties.getPackageDir()
				+ File.separator + packageMetadata.getName());
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
