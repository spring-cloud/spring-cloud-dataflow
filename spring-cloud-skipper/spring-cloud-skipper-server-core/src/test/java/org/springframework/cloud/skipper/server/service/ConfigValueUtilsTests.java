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

package org.springframework.cloud.skipper.server.service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.autoconfigure.ResourceLoadingAutoConfiguration;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.io.PackageReader;
import org.springframework.cloud.skipper.server.TestResourceUtils;
import org.springframework.cloud.skipper.server.config.SkipperServerConfiguration;
import org.springframework.cloud.skipper.server.util.ConfigValueUtils;
import org.springframework.cloud.skipper.server.util.LineUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = ConfigValueUtilsTests.TestConfig.class, properties = "spring.main.allow-bean-definition-overriding=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class ConfigValueUtilsTests {

	@Autowired
	private PackageReader packageReader;

	@Test
	void yamlMerge() throws IOException {
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(dumperOptions), dumperOptions);

		Resource resource = new ClassPathResource("/org/springframework/cloud/skipper/server/service/ticktock-1.0.0");

		Package pkg = this.packageReader.read(resource.getFile());
		ConfigValues configValues = new ConfigValues();
		Map<String, Object> configValuesMap = new TreeMap<>();
		Map<String, Object> logMap = new TreeMap<>();
		logMap.put("appVersion", "1.2.1.RELEASE");
		configValuesMap.put("log", logMap);
		configValuesMap.put("hello", "universe");

		String configYaml = yaml.dump(configValuesMap);
		configValues.setRaw(configYaml);
		Map<String, Object> mergedMap = ConfigValueUtils.mergeConfigValues(pkg, configValues);

		String mergedYaml = yaml.dump(mergedMap);
		String expectedYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "merged.yaml").getInputStream(),
				Charset.defaultCharset());
		LineUtil.assertEqualRemoveCr(mergedYaml, expectedYaml);
	}

	@Configuration
	@ImportAutoConfiguration(classes = { JacksonAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
			HibernateJpaAutoConfiguration.class, StateMachineJpaRepositoriesAutoConfiguration.class,
			ResourceLoadingAutoConfiguration.class })
	@Import(SkipperServerConfiguration.class)
	static class TestConfig {
	}
}
