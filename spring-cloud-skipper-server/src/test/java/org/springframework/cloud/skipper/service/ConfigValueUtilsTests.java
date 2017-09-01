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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.skipper.TestResourceUtils;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
@ActiveProfiles("application-repo-source-test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfigValueUtilsTests {

	private static final Class<?> CLASS = ConfigValueUtils.class;

	@Autowired
	private SkipperServerProperties skipperServerProperties;

	@Autowired
	private PackageService packageService;

	@Test
	public void testYamlMerge() throws IOException {
		Resource resource = new ClassPathResource("/org/springframework/cloud/skipper/service/ticktock-1.0.0");

		Package pkg = packageService.loadPackageOnPath("classpathOrigin", resource.getFile().getPath());
		Map<String, Object> mergedMap = ConfigValueUtils.mergeConfigValues(pkg, new ConfigValues());

		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		Yaml yaml = new Yaml(dumperOptions);
		String mergedYaml = yaml.dump(mergedMap);
		String expectedYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "merged.yaml").getInputStream(),
				Charset.defaultCharset());
		assertThat(mergedYaml).isEqualTo(expectedYaml);
	}
}
