/*
 * Copyright 2016 the original author or authors.
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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.core.io.ClassPathResource;

/**
 * Unit tests for {@link ApplicationConfigurationMetadataResolver}.
 *
 * @author Eric Bottard
 */
public class BootApplicationConfigurationMetadataResolverTests {

	private ApplicationConfigurationMetadataResolver resolver = new BootApplicationConfigurationMetadataResolver();

	@Test
	public void appSpecificWhitelistedPropsShouldBeVisible() {
		List<ConfigurationMetadataProperty> properties = resolver.listProperties(new ClassPathResource("apps/filter-processor", getClass()));
		assertThat(properties, hasItem(configPropertyIdentifiedAs("filter.expression")));
		assertThat(properties, hasItem(configPropertyIdentifiedAs("some.other.property.whitelisted.prefix.expresso2")));
	}

	@Test
	public void otherPropertiesShouldOnlyBeVisibleInExtensiveCall() {
		List<ConfigurationMetadataProperty> properties = resolver.listProperties(new ClassPathResource("apps/filter-processor", getClass()));
		assertThat(properties, not(hasItem(configPropertyIdentifiedAs("some.prefix.hidden.by.default.secret"))));
		properties = resolver.listProperties(new ClassPathResource("apps/filter-processor", getClass()), true);
		assertThat(properties, hasItem(configPropertyIdentifiedAs("some.prefix.hidden.by.default.secret")));
	}

	@Test
	public void shouldReturnEverythingWhenNoDescriptors() {
		List<ConfigurationMetadataProperty> properties = resolver.listProperties(new ClassPathResource("apps/no-whitelist", getClass()));
		List<ConfigurationMetadataProperty> full = resolver.listProperties(new ClassPathResource("apps/no-whitelist", getClass()), true);
		assertThat( properties.size(), greaterThan(0));
		assertThat(properties.size(), is(full.size()));
	}

	private Matcher<ConfigurationMetadataProperty> configPropertyIdentifiedAs(String name) {
		return hasProperty("id", is(name));
	}

}
