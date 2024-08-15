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

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public class PackageReaderTests {

	@Test
	public void read() throws IOException {
		Resource resource = new ClassPathResource("/repositories/sources/test/ticktock/ticktock-1.0.0");
		PackageReader packageReader = new DefaultPackageReader();

		Package pkg = packageReader.read(resource.getFile());
		assertThat(pkg).isNotNull();
		assertTickTockPackage(pkg);
	}

	@SuppressWarnings("unchecked")
	private void assertTickTockPackage(Package pkg) {
		PackageMetadata metadata = pkg.getMetadata();
		assertThat(metadata.getApiVersion()).isEqualTo("skipper.spring.io/v1");
		assertThat(metadata.getKind()).isEqualTo("SkipperPackageMetadata");
		assertThat(metadata.getName()).isEqualTo("ticktock");
		assertThat(metadata.getVersion()).isEqualTo("1.0.0");
		assertThat(metadata.getPackageSourceUrl()).isEqualTo("https://example.com/dataflow/ticktock");
		assertThat(metadata.getPackageHomeUrl()).isEqualTo("https://example.com/dataflow/ticktock");
		Set<String> tagSet = convertToSet(metadata.getTags());
		assertThat(tagSet).hasSize(3).contains("stream", "time", "log");
		assertThat(metadata.getMaintainer()).isEqualTo("https://github.com/markpollack");
		assertThat(metadata.getDescription()).isEqualTo("The ticktock stream sends a time stamp and logs the value.");
		String rawYamlString = pkg.getConfigValues().getRaw();
		LoaderOptions options = new LoaderOptions();
		Yaml yaml = new Yaml(new SafeConstructor(options));
		Map<String, String> valuesAsMap = (Map<String, String>) yaml.load(rawYamlString);
		assertThat(valuesAsMap).hasSize(2).containsEntry("foo", "bar").containsEntry("biz", "baz");

		assertThat(pkg.getDependencies()).hasSize(2);
		assertTimeOrLogPackage(pkg.getDependencies().get(0));
		assertTimeOrLogPackage(pkg.getDependencies().get(1));

	}

	private void assertTimeOrLogPackage(Package dependentPackage0) {
		if (dependentPackage0.getMetadata().getName().equals("time")) {
			assertTimePackage(dependentPackage0);
		}
		else {
			assertLogPackage(dependentPackage0);
		}
	}

	private void assertLogPackage(Package pkg) {
		PackageMetadata metadata = pkg.getMetadata();
		assertThat(metadata.getApiVersion()).isEqualTo("skipper.spring.io/v1");
		assertThat(metadata.getKind()).isEqualTo("SkipperPackageMetadata");
		assertThat(metadata.getName()).isEqualTo("log");
		assertThat(metadata.getVersion()).isEqualTo("2.0.0");
		assertThat(metadata.getPackageSourceUrl())
				.isEqualTo("https://github.com/spring-cloud-stream-app-starters/log/tree/v1.2.0.RELEASE");
		assertThat(metadata.getPackageHomeUrl()).isEqualTo("https://cloud.spring.io/spring-cloud-stream-app-starters/");
		Set<String> tagSet = convertToSet(metadata.getTags());
		assertThat(tagSet).hasSize(2).contains("logging", "sink");
		assertThat(metadata.getMaintainer()).isEqualTo("https://github.com/sobychacko");
		assertThat(metadata.getDescription())
				.isEqualTo("The log sink uses the application logger to output the data for inspection.");
	}

	private void assertTimePackage(Package pkg) {
		PackageMetadata metadata = pkg.getMetadata();
		assertThat(metadata.getApiVersion()).isEqualTo("skipper.spring.io/v1");
		assertThat(metadata.getKind()).isEqualTo("SkipperPackageMetadata");
		assertThat(metadata.getName()).isEqualTo("time");
		assertThat(metadata.getVersion()).isEqualTo("2.0.0");
		assertThat(metadata.getPackageSourceUrl())
				.isEqualTo("https://github.com/spring-cloud-stream-app-starters/time/tree/v1.2.0.RELEASE");
		assertThat(metadata.getPackageHomeUrl()).isEqualTo("https://cloud.spring.io/spring-cloud-stream-app-starters/");
		Set<String> tagSet = convertToSet(metadata.getTags());
		assertThat(tagSet).hasSize(2).contains("time", "source");
		assertThat(metadata.getMaintainer()).isEqualTo("https://github.com/sobychacko");
		assertThat(metadata.getDescription()).isEqualTo("The time source periodically emits a timestamp string.");
	}

	private Set<String> convertToSet(String tags) {
		Set<String> initialSet = StringUtils.commaDelimitedListToSet(tags);

		Set<String> setToReturn = initialSet.stream()
				.map(StringUtils::trimAllWhitespace)
				.collect(Collectors.toSet());

		return setToReturn;
	}
}
