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
package org.springframework.cloud.skipper.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.zeroturnaround.zip.commons.FileUtils;

import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.util.Assert;

/**
 * @author Mark Pollack
 */
public class DefaultPackageReader implements PackageReader {

	@Override
	public Package read(File packageDirectory) {
		Assert.notNull(packageDirectory, "File to load package from can not be null");
		List<File> files;
		try (Stream<Path> paths = Files.walk(Paths.get(packageDirectory.getPath()), 1)) {
			files = paths.map(i -> i.toAbsolutePath().toFile()).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not process files in path " + packageDirectory.getPath(), e);
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
			if (file.getName().equalsIgnoreCase("values.yaml") ||
					file.getName().equalsIgnoreCase("values.yml")) {
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
				File[] dependentPackageDirectories = file.listFiles();
				List<Package> dependencies = new ArrayList<>();
				for (File dependentPackageDirectory : dependentPackageDirectories) {
					dependencies.add(read(dependentPackageDirectory));
				}
				pkg.setDependencies(dependencies);
			}
		}
		return pkg;
	}

	private List<Template> loadTemplates(File templatePath) {
		List<File> files;
		try (Stream<Path> paths = Files.walk(Paths.get(templatePath.getAbsolutePath()), 1)) {
			files = paths.map(i -> i.toAbsolutePath().toFile()).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not process files in template path " + templatePath, e);
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
					throw new IllegalArgumentException("Could read template file " + file.getAbsoluteFile(), e);
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

	private ConfigValues loadConfigValues(File file) {
		ConfigValues configValues = new ConfigValues();
		try {
			configValues.setRaw(new String(Files.readAllBytes(file.toPath()), "UTF-8"));
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could read values file " + file.getAbsoluteFile(), e);
		}
		return configValues;
	}

	private PackageMetadata loadPackageMetadata(File file) {
		Yaml yaml = new Yaml(new Constructor(PackageMetadata.class));
		String fileContents = null;
		try {
			fileContents = FileUtils.readFileToString(file);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		PackageMetadata pkgMetadata = (PackageMetadata) yaml.load(fileContents);
		return pkgMetadata;
	}
}
