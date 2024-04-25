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

package org.springframework.cloud.skipper.server.templates;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.autoconfigure.ResourceLoadingAutoConfiguration;
import org.springframework.cloud.skipper.server.config.SkipperServerConfiguration;
import org.springframework.cloud.skipper.server.templates.PackageTemplateTests.TestConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;


/**
 * @author Mark Pollack
 * @author Chris Bono
 */
@SpringBootTest(classes = TestConfig.class, properties = "spring.main.allow-bean-definition-overriding=true")
public class PackageTemplateTests {

	private final Logger logger = LoggerFactory.getLogger(PackageTemplateTests.class);

	@Value("classpath:templates/packageUsingNestedMaps.yml")
	private Resource nestedMapResource;

	@Value("classpath:templates/values.yml")
	private Resource valuesResource;

	@Test
	@SuppressWarnings("unchecked")
	public void testMustasche() throws IOException {
		Yaml yaml = new Yaml(new SafeConstructor());
		Map model = yaml.load(valuesResource.getInputStream());
		String templateAsString = StreamUtils.copyToString(nestedMapResource.getInputStream(),
				Charset.defaultCharset());
		Template mustacheTemplate = Mustache.compiler().compile(templateAsString);
		String resolvedYml = mustacheTemplate.execute(model);
		Map map = yaml.load(resolvedYml);

		logger.info("Resolved yml = " + resolvedYml);
		assertThat(map).containsKeys("apiVersion", "deployment");
		Map deploymentMap = (Map) map.get("deployment");
		assertThat(deploymentMap).contains(entry("name", "time"))
				.contains(entry("count", 10));
		Map applicationProperties = (Map) deploymentMap.get("applicationProperties");
		assertThat(applicationProperties).contains(entry("log.level", "DEBUG"), entry("server.port", 8089));
		Map deploymentProperties = (Map) deploymentMap.get("deploymentProperties");
		assertThat(deploymentProperties).contains(entry("app.time.producer.partitionKeyExpression", "payload"),
				entry("app.log.spring.cloud.stream.bindings.input.consumer.maxAttempts", 5));
	}

	@Configuration
	@ImportAutoConfiguration(classes = { JacksonAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
			HibernateJpaAutoConfiguration.class, StateMachineJpaRepositoriesAutoConfiguration.class,
			ResourceLoadingAutoConfiguration.class })
	@Import(SkipperServerConfiguration.class)
	static class TestConfig {
	}
}
