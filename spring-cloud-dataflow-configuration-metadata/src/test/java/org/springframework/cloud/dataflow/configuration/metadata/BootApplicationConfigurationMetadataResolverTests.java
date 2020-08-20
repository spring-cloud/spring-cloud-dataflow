/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataResolver;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApplicationConfigurationMetadataResolver}.
 *
 * @author Eric Bottard
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
public class BootApplicationConfigurationMetadataResolverTests {

	@Mock
	private ContainerImageMetadataResolver containerImageMetadataResolver;

	private ApplicationConfigurationMetadataResolver resolver;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		resolver = new BootApplicationConfigurationMetadataResolver(containerImageMetadataResolver);
	}

	@Test
	public void appDockerResourceEmptyLabels() {
		when(containerImageMetadataResolver.getImageLabels("test/test:latest")).thenReturn(new HashMap<>());
		List<ConfigurationMetadataProperty> properties = resolver
				.listProperties(new DockerResource("test/test:latest"));
		assertThat(properties.size(), is(0));
	}

	@Test
	public void appDockerResource() throws IOException {
		byte[] bytes = StreamUtils.copyToByteArray(new ClassPathResource(
				"apps/no-visible-properties/META-INF/spring-configuration-metadata.json", getClass())
						.getInputStream());
		when(containerImageMetadataResolver.getImageLabels("test/test:latest"))
				.thenReturn(Collections.singletonMap(
						"org.springframework.cloud.dataflow.spring-configuration-metadata.json",
						new String(bytes)));
		List<ConfigurationMetadataProperty> properties = resolver
				.listProperties(new DockerResource("test/test:latest"));
		assertThat(properties.size(), is(3));
	}

	@Test
	public void appDockerResourceBrokenFormat() {
		byte[] bytes = "Invalid metadata json content1".getBytes();
		Map<String, String> result = Collections.singletonMap(
				"org.springframework.cloud.dataflow.spring-configuration-metadata.json",
				new String(bytes));
		when(containerImageMetadataResolver.getImageLabels("test/test:latest")).thenReturn(result);
		List<ConfigurationMetadataProperty> properties = resolver
				.listProperties(new DockerResource("test/test:latest"));
		assertThat(properties.size(), is(0));
	}

	@Test
	public void appSpecificVisiblePropsShouldBeVisible() {
		List<ConfigurationMetadataProperty> properties = resolver
				.listProperties(new ClassPathResource("apps/filter-processor", getClass()));
		assertThat(properties, hasItem(configPropertyIdentifiedAs("filter.expression")));
		assertThat(properties, hasItem(configPropertyIdentifiedAs("some.other.property.included.prefix.expresso2")));
	}

	@Test
	public void appSpecificVisibleLegacyPropsShouldBeVisible() {
		List<ConfigurationMetadataProperty> properties = resolver
				.listProperties(new ClassPathResource("apps/filter-processor-legacy", getClass()));
		assertThat(properties, hasItem(configPropertyIdentifiedAs("filter.expression")));
		assertThat(properties, hasItem(configPropertyIdentifiedAs("some.other.property.included.prefix.expresso2")));
	}

	@Test
	public void appSpecificVisibleLegacyPropsShouldBeVisibleIfBothInPlace() {
		// test resource files has both expresso2 and expresso3 in spring-configuration-metadata
		// and as we prefer new format(expresso3 included) and it exists
		// expresso2 from old format doesn't get read.
		List<ConfigurationMetadataProperty> properties = resolver
				.listProperties(new ClassPathResource("apps/filter-processor-both", getClass()));
		assertThat(properties, hasItem(configPropertyIdentifiedAs("filter.expression")));
		assertThat(properties, hasItem(configPropertyIdentifiedAs("some.other.property.included.prefix.expresso3")));
		assertThat(properties, hasItem(configPropertyIdentifiedAs("some.other.property.included.prefix.expresso2")));
	}

	@Test
	public void otherPropertiesShouldOnlyBeVisibleInExtensiveCall() {
		List<ConfigurationMetadataProperty> properties = resolver
				.listProperties(new ClassPathResource("apps/filter-processor", getClass()));
		assertThat(properties, not(hasItem(configPropertyIdentifiedAs("some.prefix.hidden.by.default.secret"))));
		properties = resolver.listProperties(new ClassPathResource("apps/filter-processor", getClass()), true);
		assertThat(properties, hasItem(configPropertyIdentifiedAs("some.prefix.hidden.by.default.secret")));
	}

	@Test
	public void shouldReturnEverythingWhenNoDescriptors() {
		List<ConfigurationMetadataProperty> properties = resolver
				.listProperties(new ClassPathResource("apps/no-visible-properties", getClass()));
		List<ConfigurationMetadataProperty> full = resolver
				.listProperties(new ClassPathResource("apps/no-visible-properties", getClass()), true);
		assertThat(properties.size(), is(0));
		assertThat(full.size(), is(3));
	}

	@Test
	public void deprecatedErrorPropertiesShouldNotBeVisible() {
		List<ConfigurationMetadataProperty> properties = resolver
				.listProperties(new ClassPathResource("apps/deprecated-error", getClass()));
		List<ConfigurationMetadataProperty> full = resolver
				.listProperties(new ClassPathResource("apps/deprecated-error", getClass()), true);
		assertThat(properties.size(), is(0));
		assertThat(full.size(), is(2));
	}

	@Test
	public void appDockerResourceWithInboundOutboundPortMapping() {
		Map<String, String> result = new HashMap<>();
		result.put("configuration-properties.inbound-ports", "input1,input2, input3");
		result.put("configuration-properties.outbound-ports", "output1, output2");
		when(this.containerImageMetadataResolver.getImageLabels("test/test:latest")).thenReturn(result);
		Map<String, Set<String>> portNames = this.resolver.listPortNames(new DockerResource("test/test:latest"));
		assertThat(portNames.size(), is(2));
		assertThat(portNames.get("inbound").size(), is(3));
		assertThat(portNames.get("inbound"), containsInAnyOrder("input1", "input2", "input3"));
		assertThat(portNames.get("outbound").size(), is(2));
		assertThat(portNames.get("outbound"), containsInAnyOrder("output1", "output2"));
	}

	private Matcher<ConfigurationMetadataProperty> configPropertyIdentifiedAs(String name) {
		return hasProperty("id", is(name));
	}

}
