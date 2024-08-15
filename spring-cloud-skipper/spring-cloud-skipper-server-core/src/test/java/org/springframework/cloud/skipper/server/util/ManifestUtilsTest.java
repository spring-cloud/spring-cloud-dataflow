/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.skipper.server.util;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.io.DefaultPackageReader;
import org.springframework.cloud.skipper.io.PackageReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
public class ManifestUtilsTest {

	@Test
	public void testCreateManifest() throws IOException {
		Resource resource = new ClassPathResource("/repositories/sources/test/ticktock/ticktock-1.0.1");
		PackageReader packageReader = new DefaultPackageReader();

		Package pkg = packageReader.read(resource.getFile());
		assertThat(pkg).isNotNull();

		Date date = new Date(666);
		Map<String, Object> log = new HashMap<>();
		log.put("version", "666");
		log.put("adate", new Date(666));
		log.put("bool", true);
		log.put("array", "[a, b, c]");

		Map<String, String> time = new HashMap<>();
		time.put("version", "666");

		Map<String, Object> map = new HashMap<>();
		map.put("log", log);
		map.put("time", time);

		String manifest = ManifestUtils.createManifest(pkg, map);

		String dateAsStringWithQuotes = "\"" + date.toString() + "\"";

		assertThat(manifest).contains("\"version\": \"666\"").describedAs("Handle Integer");
		assertThat(manifest).contains("\"bool\": \"true\"").describedAs("Handle Boolean");
		assertThat(manifest).contains("\"adate\": " + dateAsStringWithQuotes).describedAs("Handle Date");
		assertThat(manifest).contains("\"array\":\n  - \"a\"\n  - \"b\"\n  - \"c\"").describedAs("Handle Array");
		assertThat(manifest).contains("\"deploymentProperties\": !!null \"null\"").describedAs("Handle Null");
	}
}
