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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.SkipperUtils;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.io.PackageReader;
import org.springframework.cloud.skipper.io.TempFileUtils;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.repository.RepositoryRepository;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
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

	private final RepositoryRepository repositoryRepository;

	private final PackageMetadataRepository packageMetadataRepository;

	private final PackageReader packageReader;

	private ResourceLoader resourceLoader;

	@Autowired
	public PackageService(RepositoryRepository repositoryRepository,
			PackageMetadataRepository packageMetadataRepository,
			PackageReader packageReader) {
		this.repositoryRepository = repositoryRepository;
		this.packageMetadataRepository = packageMetadataRepository;
		this.packageReader = packageReader;
	}

	public Package downloadPackage(PackageMetadata packageMetadata) {
		Assert.notNull(packageMetadata, "Can't download PackageMetadata, it is a null value.");
		// Database contains the package file from a previous upload
		if (packageMetadata.getPackageFileBytes() != null) {
			return deserializePackageFromDatabase(packageMetadata);
		}
		else {
			return downloadAndDeserializePackage(packageMetadata);
		}
	}

	private Package downloadAndDeserializePackage(PackageMetadata packageMetadata) {
		Path targetPath = null;
		// package file is in a non DB hosted repository
		try {
			targetPath = TempFileUtils.createTempDirectory("skipper" + packageMetadata.getName());
			File targetFile = SkipperUtils.calculatePackageZipFile(packageMetadata, targetPath.toFile());
			logger.debug("Finding repository for package origin {}", packageMetadata.getOrigin());
			Repository packageRepository = repositoryRepository.findOne(packageMetadata.getOrigin());
			if (packageRepository == null) {
				return throwDescriptiveException(packageMetadata);
			}
			Resource sourceResource = getResourceForRepository(packageRepository, packageMetadata.getName(),
					packageMetadata.getVersion());

			logger.debug("Downloading package file for {}-{} from {} to target file {}",
					packageMetadata.getName(), packageMetadata.getVersion(), sourceResource.getDescription(),
					targetFile);
			try {
				StreamUtils.copy(sourceResource.getInputStream(), new FileOutputStream(targetFile));
			}
			catch (IOException e) {
				throw new SkipperException("Could not copy package file for " + packageMetadata.getName() + "-"
						+ packageMetadata.getVersion() +
						" from " + sourceResource.getDescription() + " to target file " + targetFile, e);
			}
			ZipUtil.unpack(targetFile, targetPath.toFile());
			Package pkgToReturn = this.packageReader
					.read(new File(targetPath.toFile(), packageMetadata.getName() + "-" +
							packageMetadata.getVersion()));
			// TODO should we have an option to not cache the package file?
			packageMetadata.setPackageFileBytes(Files.readAllBytes(targetFile.toPath()));
			// Only save once package is successfully deserialized and package file read.
			pkgToReturn.setMetadata(this.packageMetadataRepository.save(packageMetadata));
			return pkgToReturn;
		}
		catch (IOException ex) {
			throw new SkipperException("Exception while downloading package zip file for "
					+ packageMetadata.getName() + "-" + packageMetadata.getVersion() +
					". PackageMetadata origin = " + packageMetadata.getOrigin(), ex);
		}
		catch (InvalidDataAccessApiUsageException ex) {
			throw new SkipperException("Exception while downloading package zip file for "
					+ packageMetadata.getName() + "-" + packageMetadata.getVersion() +
					". PackageMetadata origin = " + packageMetadata.getOrigin() + "No repository found.", ex);
		}
		catch (Exception ex) {
			logger.error("This is a catch all debug statement.", ex);
			throw new SkipperException("Catch all", ex);
		}
		finally {
			if (targetPath != null && !FileSystemUtils.deleteRecursively(targetPath.toFile())) {
				logger.warn("Temporary directory can not be deleted: " + targetPath);
			}
		}
	}

	private Package throwDescriptiveException(PackageMetadata packageMetadata) {
		List<Repository> list = StreamSupport
				.stream(repositoryRepository.findAll().spliterator(), false)
				.collect(Collectors.toList());
		throw new SkipperException("Can not find packageRepository for origin = "
				+ packageMetadata.getOrigin() + ". Known repositories are " + Arrays.toString(list.toArray()));
	}

	private Package deserializePackageFromDatabase(PackageMetadata packageMetadata) {
		// package file was uploaded to a local DB hosted repository
		Path tmpDirPath = null;
		try {
			tmpDirPath = TempFileUtils.createTempDirectory("skipper");
			File targetPath = new File(tmpDirPath + File.separator + packageMetadata.getName());
			targetPath.mkdirs();
			File targetFile = SkipperUtils.calculatePackageZipFile(packageMetadata, targetPath);
			try {
				StreamUtils.copy(packageMetadata.getPackageFileBytes(), new FileOutputStream(targetFile));
			}
			catch (IOException e) {
				throw new SkipperException(
						"Could not copy package file for " + packageMetadata.getName() + "-"
								+ packageMetadata.getVersion() +
								" from database to target file " + targetFile,
						e);
			}
			ZipUtil.unpack(targetFile, targetPath);
			Package pkgToReturn = this.packageReader.read(new File(targetPath, packageMetadata.getName() + "-" +
					packageMetadata.getVersion()));
			pkgToReturn.setMetadata(packageMetadata);
			return pkgToReturn;
		}
		finally {
			if (tmpDirPath != null && !FileSystemUtils.deleteRecursively(tmpDirPath.toFile())) {
				logger.warn("Temporary directory can not be deleted: " + tmpDirPath);
			}
		}
	}

	private Resource getResourceForRepository(Repository packageRepository, String name, String version) {
		// TODO local respository will not have url, add assertion
		String sourceUrl = packageRepository.getUrl() + "/" + name + "/" +
				name + "-" + version + ".zip";
		logger.debug("PackageRepository.getUrl={}, Attempting to get resource at URL {} ", packageRepository.getUrl(),
				sourceUrl);
		Resource resource = resourceLoader.getResource(sourceUrl);
		if (resource.exists()) {
			return resource;
		}
		throw new SkipperException("Resource " + name + "-" + version + " in package repository "
				+ packageRepository.getName() + " does not exist.");
	}

	public PackageMetadata upload(UploadRequest uploadRequest) {
		validateUploadRequest(uploadRequest);
		Repository localRepositoryToUpload = getRepositoryToUpload(uploadRequest.getRepoName());
		Path packageDirPath = null;
		try {
			packageDirPath = TempFileUtils.createTempDirectory("skipperUpload");
			File packageDir = new File(packageDirPath + File.separator + uploadRequest.getName());
			packageDir.mkdir();
			Path packageFile = Paths
					.get(packageDir.getPath() + File.separator + uploadRequest.getName() + "-"
							+ uploadRequest.getVersion() + "." + uploadRequest.getExtension());
			Assert.isTrue(packageDir.exists(), "Package directory doesn't exist.");
			Files.write(packageFile, uploadRequest.getPackageFileAsBytes());
			ZipUtil.unpack(packageFile.toFile(), packageDir);
			String unzippedPath = packageDir.getAbsolutePath() + File.separator + uploadRequest.getName()
					+ "-" + uploadRequest.getVersion();
			File unpackagedFile = new File(unzippedPath);
			Assert.isTrue(unpackagedFile.exists(), "Package is expected to be unpacked, but it doesn't exist");
			Package packageToUpload = this.packageReader.read(unpackagedFile);
			PackageMetadata packageMetadata = packageToUpload.getMetadata();
			// TODO: Model the PackageMetadata -> Repository relationship in the DB.
			if (localRepositoryToUpload != null) {
				packageMetadata.setOrigin(localRepositoryToUpload.getId());
			}
			packageMetadata.setPackageFileBytes(uploadRequest.getPackageFileAsBytes());
			return this.packageMetadataRepository.save(packageMetadata);
		}
		catch (IOException e) {
			throw new SkipperException("Failed to upload the package " + e.getCause());
		}
		finally {
			if (packageDirPath != null && !FileSystemUtils.deleteRecursively(packageDirPath.toFile())) {
				logger.warn("Temporary directory can not be deleted: " + packageDirPath);
			}
		}
	}

	private Repository getRepositoryToUpload(String repoName) {
		Repository localRepositoryToUpload = this.repositoryRepository.findByName(repoName);
		// todo: should we enforce the existence of the local repository always?
		// TODO: Verify the repo name set to package upload properties always belong to local
		// repository type.
		return localRepositoryToUpload;
	}

	private void validateUploadRequest(UploadRequest uploadRequest) {
		Assert.notNull(uploadRequest.getRepoName(), "Repo name can not be null");
		Assert.notNull(uploadRequest.getName(), "Name of package can not be null");
		// TODO assert is semver format
		Assert.notNull(uploadRequest.getVersion(), "Version can not be null");
		Assert.notNull(uploadRequest.getExtension(), "Extension can not be null");
		Assert.isTrue(uploadRequest.getExtension().equals("zip"), "Extension must be 'zip', not "
				+ uploadRequest.getExtension());
		Assert.notNull(uploadRequest.getPackageFileAsBytes(), "Package file as bytes must not be null");
		Assert.isTrue(uploadRequest.getPackageFileAsBytes().length != 0, "Package file as bytes must not be empty");
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
