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
package org.springframework.cloud.skipper.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class DefaultPackageWriter implements PackageWriter {

	private Yaml yaml;

	public DefaultPackageWriter() {
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		this.yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(dumperOptions), dumperOptions);

	}

	@Override
	public File write(Package pkg, File targetDirectory) {
		PackageMetadata packageMetadata = pkg.getMetadata();
		File tmpDir = TempFileUtils.createTempDirectory("skipper" + packageMetadata.getName()).toFile();
		File rootPackageDir = new File(tmpDir,
				String.format("%s-%s", packageMetadata.getName(), packageMetadata.getVersion()));
		rootPackageDir.mkdir();
		writePackage(pkg, rootPackageDir);
		if (!pkg.getDependencies().isEmpty()) {
			File packagesDir = new File(rootPackageDir, "packages");
			packagesDir.mkdir();
			for (Package dependencyPkg : pkg.getDependencies()) {
				File packageDir = new File(packagesDir, dependencyPkg.getMetadata().getName());
				packageDir.mkdir();
				writePackage(dependencyPkg, packageDir);
			}
		}
		File targetZipFile = PackageFileUtils.calculatePackageZipFile(pkg.getMetadata(), targetDirectory);
		ZipUtil.pack(rootPackageDir, targetZipFile, true);
		FileSystemUtils.deleteRecursively(tmpDir);
		return targetZipFile;
	}

	private void writePackage(Package pkg, File directory) {
		String packageMetadata = generatePackageMetadata(pkg.getMetadata());
		writeText(new File(directory, "package.yml"), packageMetadata);
		if (pkg.getConfigValues() != null && StringUtils.hasText(pkg.getConfigValues().getRaw())) {
			writeText(new File(directory, "values.yml"), pkg.getConfigValues().getRaw());
		}
		if (!pkg.getTemplates().isEmpty()) {
			File templateDir = new File(directory, "templates/");
			templateDir.mkdirs();
			File templateFile = new File(templateDir, pkg.getMetadata().getName() + ".yml");
			writeText(templateFile, getDefaultTemplate());
		}
	}

	private String getDefaultTemplate() {
		Resource resource = new ClassPathResource("/org/springframework/cloud/skipper/io/generic-template.yml");
		String genericTempateData = null;
		try {
			genericTempateData = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't load generic template", e);
		}
		return genericTempateData;
	}

	private void writeText(File target, String body) {
		try (OutputStream stream = new FileOutputStream(target)) {
			StreamUtils.copy(body, Charset.forName("UTF-8"), stream);
		}
		catch (Exception e) {
			throw new IllegalStateException("Cannot write file " + target, e);
		}
	}

	private String generatePackageMetadata(PackageMetadata packageMetadata) {
		return yaml.dump(packageMetadata);
	}

}
