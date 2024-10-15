/*
 * Copyright 2017-2022 the original author or authors.
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.cloud.skipper.TestResourceUtils;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
class PackageWriterTests {

	@Test
	void test() throws IOException {
		PackageWriter packageWriter = new DefaultPackageWriter();
		Package pkgtoWrite = createSimplePackage();
		Path tempPath = Files.createTempDirectory("tests");
		File outputDirectory = tempPath.toFile();

		File zipFile = packageWriter.write(pkgtoWrite, outputDirectory);
		assertThat(zipFile).exists();
		assertThat(zipFile).hasName("myapp-1.0.0.zip");
		final AtomicInteger processedEntries = new AtomicInteger(3);
		ZipUtil.iterate(zipFile, (inputStream, zipEntry) -> {
			if (zipEntry.getName().equals("myapp-1.0.0/package.yml")) {
				assertExpectedContents(inputStream, "package.yml");
				processedEntries.decrementAndGet();
			}
			if (zipEntry.getName().equals("myapp-1.0.0/values.yml")) {
				assertExpectedContents(inputStream, "values.yml");
				processedEntries.decrementAndGet();
			}
			if (zipEntry.getName().equals("myapp-1.0.0/templates/myapp.yml")) {
				assertExpectedContents(inputStream, "generic-template.yml");
				processedEntries.decrementAndGet();
			}
		});
		// make sure we asserted all fields
		assertThat(processedEntries.get()).isEqualTo(0);
	}

	private void assertExpectedContents(InputStream zipEntryInputStream, String resourceSuffix) throws IOException {
		String zipEntryAsString = StreamUtils.copyToString(zipEntryInputStream, Charset.defaultCharset());
		String expectedYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), resourceSuffix)
						.getInputStream(),
				Charset.defaultCharset());
		assertThat(removeCr(zipEntryAsString)).isEqualTo(removeCr(expectedYaml));
	}

	private Package createSimplePackage() throws IOException {
		Package pkg = new Package();

		// Add package metadata
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName("myapp");
		packageMetadata.setVersion("1.0.0");
		packageMetadata.setMaintainer("bob");
		pkg.setMetadata(packageMetadata);

		// Add ConfigValues
		Map<String, String> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("fiz", "faz");
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(dumperOptions), dumperOptions);
		ConfigValues configValues = new ConfigValues();
		configValues.setRaw(yaml.dump(map));
		pkg.setConfigValues(configValues);

		// Add template
		Resource resource = new ClassPathResource("/org/springframework/cloud/skipper/io/generic-template.yml");
		String genericTempateData = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
		Template template = new Template();
		template.setData(genericTempateData);
		template.setName(resource.getURL().toString());
		List<Template> templateList = new ArrayList<>();
		templateList.add(template);
		pkg.setTemplates(templateList);

		return pkg;
	}

	private static String removeCr(final String input) {
		return input != null ? input.replace("\r\n", "\n") : null;
	}
}
