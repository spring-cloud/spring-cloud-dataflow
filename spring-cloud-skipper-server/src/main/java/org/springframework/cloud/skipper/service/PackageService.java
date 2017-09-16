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

import io.jsonwebtoken.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.YamlConfigurationFactory;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.PackageUploadProperties;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.index.PackageException;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.repository.RepositoryRepository;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;

/**
 * Service responsible for downloading package .zip files and loading them into the
 * Package object.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@Service
public class PackageService implements ResourceLoaderAware {

	private final Logger logger = LoggerFactory.getLogger(PackageService.class);

	private ResourceLoader resourceLoader;

	private RepositoryRepository repositoryRepository;

	private PackageMetadataRepository packageMetadataRepository;

	@Autowired
	public PackageService(RepositoryRepository repositoryRepository,
			PackageMetadataRepository packageMetadataRepository) {
		this.repositoryRepository = repositoryRepository;
		this.packageMetadataRepository = packageMetadataRepository;
	}

	public Package downloadPackage(PackageMetadata packageMetadata) {
		Assert.notNull(packageMetadata, "Can't download PackageMetadata, it is a null value.");
		File targetPath = null;
		File targetFile = null;
		Package downloadedPackage = null;
		Path tmpDirPath = null;
		try {
			if (packageMetadata.getPackageFile() != null) {
				tmpDirPath = Files.createTempDirectory("skipper");
				targetPath = new File(tmpDirPath + File.separator + packageMetadata.getName());
				targetPath.mkdirs();
				targetFile = calculatePackageZipFile(packageMetadata, targetPath);
				try {
					StreamUtils.copy(packageMetadata.getPackageFile(), new FileOutputStream(targetFile));
				}
				catch (IOException e) {
					throw new PackageException(
							"Could not copy " + packageMetadata.getPackageFile() + " to " + targetFile, e);
				}
			}
			else {
				Resource sourceResource = findFirstPackageResourceThatExists(packageMetadata.getName(),
						packageMetadata.getVersion());
				targetPath = calculatePackageDownloadDirectory(packageMetadata);
				targetPath.mkdirs();
				targetFile = calculatePackageZipFile(packageMetadata, targetPath);
				try {
					StreamUtils.copy(sourceResource.getInputStream(), new FileOutputStream(targetFile));
					logger.info("Downloaded package [" + packageMetadata.getName() + "-" + packageMetadata.getVersion()
							+ "] from " + sourceResource.getURL());
				}
				catch (IOException e) {
					throw new PackageException("Could not copy " + sourceResource + " to " + targetFile, e);
				}
				packageMetadata.setPackageFile(Files.readAllBytes(targetFile.toPath()));
				this.packageMetadataRepository.save(packageMetadata);
			}
			ZipUtil.unpack(targetFile, targetPath);
			return loadPackageOnPath(new File(targetPath, packageMetadata.getName() + "-" +
					packageMetadata.getVersion()));
		}
		catch (IOException e) {
			throw new PackageException("Exception while setting PackageMetadata");
		}
		finally {
			if (tmpDirPath != null && !FileSystemUtils.deleteRecursively(tmpDirPath.toFile())) {
				logger.warn("Temporary directory can not be deleted: " + tmpDirPath);
			}
		}
	}

	private Resource findFirstPackageResourceThatExists(String name, String version) {
		Assert.notNull(name, "name can not be null");
		Assert.notNull(version, "version can not be null");
		Resource sourceResource = null;
		boolean found = false;
		for (Repository packageRepository : this.repositoryRepository.findAll()) {
			String sourceUrl = packageRepository.getUrl() + "/" + name + "/" +
					name + "-" + version + ".zip";
			sourceResource = resourceLoader.getResource(sourceUrl);
			if (sourceResource.exists()) {
				logger.debug(String.format("Found resource for Package name '%s', version '%s'.  URL = '%s' ",
						name, version, sourceUrl));
				found = true;
				break;
			}
			else {
				logger.debug(String.format("No resource for Package name '%s', version '%s' at URL = '%s' ",
						name, version, sourceUrl));
			}
		}
		if (!found) {
			throw new PackageException(String.format(
					"Resource for Package name '%s', version '%s' was not found in any repository.", name, version));
		}
		return sourceResource;
	}

	public PackageMetadata upload(PackageUploadProperties packageUploadProperties) {
		Assert.notNull(packageUploadProperties.getRepoName(), "Repo name must not be null");
		// todo: Verify the repo name set to package upload properties always belong to local repository type.
		Repository localRepositoryToUpload = this.repositoryRepository
				.findByName(packageUploadProperties.getRepoName());
		Assert.notNull(localRepositoryToUpload,
				"Local repository " + packageUploadProperties.getRepoName() + "doesn't exist.");
		Path packageFile;
		PackageMetadata packageMetadata = null;
		try {
			Path packageDirPath = Files.createTempDirectory("skipper");
			File packageDir = new File(packageDirPath + File.separator + packageUploadProperties.getName());
			packageDir.mkdir();
			packageFile = Paths
					.get(packageDir.getPath() + File.separator + packageUploadProperties.getName() + "-"
							+ packageUploadProperties.getVersion() + "." + packageUploadProperties.getExtension());
			Assert.isTrue(packageDir.exists(), "Package directory doesn't exist.");
			Files.write(packageFile, packageUploadProperties.getFileToUpload());
			if (packageUploadProperties.getExtension().contains("zip")) {
				ZipUtil.unpack(packageFile.toFile(), packageDir);
			}
			String unzippedPath = packageDir.getAbsolutePath() + File.separator + packageUploadProperties.getName()
					+ "-" + packageUploadProperties.getVersion();
			File unpackagedFile = new File(unzippedPath);
			Assert.isTrue(unpackagedFile.exists(), "Package is expected to be unpacked, but it doesn't exist");
			Package packageToUpload = loadPackageOnPath(unpackagedFile);
			packageMetadata = packageToUpload.getMetadata();
			// todo: Model the PackageMetadata -> Repository relationship in the DB.
			packageMetadata.setOrigin(localRepositoryToUpload.getId());
			packageMetadata.setPackageFile(packageUploadProperties.getFileToUpload());
			if (!FileSystemUtils.deleteRecursively(packageDirPath.toFile())) {
				logger.warn("Temporary directory can not be deleted: " + packageDirPath);
			}
			this.packageMetadataRepository.save(packageMetadata);
		}
		catch (IOException e) {
			throw new PackageException("Sorry, failed to upload the package " + e.getCause());
		}
		return packageMetadata;
	}

	protected Package loadPackageOnPath(File unpackedPackage) {
		List<File> files;
		try (Stream<Path> paths = Files.walk(Paths.get(unpackedPackage.getPath()), 1)) {
			files = paths.map(i -> i.toAbsolutePath().toFile()).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new PackageException("Could not process files in path " + unpackedPackage.getPath(), e);
		}
		Package pkg = new Package();
		// Iterate over all files and "deserialize" the package.
		for (File file : files) {
			// Package metadata
			if (file.getName().equalsIgnoreCase("package.yaml") || file.getName().equalsIgnoreCase("package.yml")) {
				pkg.setMetadata(loadPackageMetadata(file));
				continue;
			}
			// Package property values for configuration
			if (file.getName().equalsIgnoreCase("values.yaml") || file.getName().equalsIgnoreCase("values.yml")) {
				pkg.setConfigValues(loadConfigValues(file));
				continue;
			}
			// The template files
			String absFileName = file.getAbsoluteFile().toString();
			if (absFileName.endsWith("/templates")) {
				pkg.setTemplates(loadTemplates(file));
				continue;
			}
			// dependent packages
			if ((file.getName().equalsIgnoreCase("packages") && file.isDirectory())) {
				System.out.println("found the packages directory");
				File[] dependentPackageDirectories = file.listFiles();
				List<Package> dependencies = new ArrayList<>();
				for (File dependentPackageDirectory : dependentPackageDirectories) {
					dependencies.add(loadPackageOnPath(dependentPackageDirectory));
				}
				pkg.setDependencies(dependencies);
			}
		}
		if (!FileSystemUtils.deleteRecursively(unpackedPackage)) {
			logger.warn("Temporary directory can not be deleted: " + unpackedPackage);
		}
		return pkg;
	}

	private PackageMetadata loadPackageMetadata(File file) {
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

	/**
	 * Give the PackageMetadata, return the directory where the package will be downloaded
	 * to. The directory takes the server's PackageDir configuraiton property and appends
	 * the package name taken from the metadata.
	 * @param packageMetadata the package's metadata.
	 * @return The directory where the package will be downloaded.
	 */
	public File calculatePackageDownloadDirectory(PackageMetadata packageMetadata) {
		Repository localRepository = this.repositoryRepository
				.findByName(RepositoryInitializationService.LOCAL_REPOSITORY_NAME);
		String packagesPath = localRepository.getUrl().substring("file://".length());
		File downloadDir = new File(packagesPath, "downloads");
		return new File(downloadDir, packageMetadata.getName());
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
