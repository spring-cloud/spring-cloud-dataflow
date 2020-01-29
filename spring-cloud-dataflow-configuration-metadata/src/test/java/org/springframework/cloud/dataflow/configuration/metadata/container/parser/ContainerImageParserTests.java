/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata.container.parser;

import org.junit.Test;

import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImage;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageParser;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Christian Tzolov
 */
public class ContainerImageParserTests {

	private ContainerImageParser containerImageNameParser =
			new ContainerImageParser("test-domain.io", "tag654", "official-repo-name");

	@Test
	public void testParseWithoutDefaults() {
		ContainerImage containerImageName =
				containerImageNameParser.parse("springsource-docker-private-local.jfrog.io:80/scdf/stream/spring-cloud-dataflow-acceptance-image-drivers173:123");

		assertThat(containerImageName.getHostname(), is("springsource-docker-private-local.jfrog.io"));
		assertThat(containerImageName.getPort(), is("80"));
		assertThat(containerImageName.getRepositoryNamespace(), is("scdf/stream"));
		assertThat(containerImageName.getRepositoryName(), is("spring-cloud-dataflow-acceptance-image-drivers173"));
		assertThat(containerImageName.getRepositoryTag(), is("123"));

		assertThat(containerImageName.getRegistryHost(), is("springsource-docker-private-local.jfrog.io:80"));
		assertThat(containerImageName.getRepository(), is("scdf/stream/spring-cloud-dataflow-acceptance-image-drivers173"));

		assertThat(containerImageName.getCanonicalName(), is("springsource-docker-private-local.jfrog.io:80/scdf/stream/spring-cloud-dataflow-acceptance-image-drivers173:123"));
	}

	@Test
	public void testParseWithDefaults() {
		ContainerImage containerImageName = containerImageNameParser.parse("simple-repo-name");

		assertThat(containerImageName.getHostname(), is("test-domain.io"));
		assertNull(containerImageName.getPort());
		assertThat(containerImageName.getRepositoryNamespace(), is("official-repo-name"));
		assertThat(containerImageName.getRepositoryName(), is("simple-repo-name"));
		assertThat(containerImageName.getRepositoryTag(), is("tag654"));

		assertThat(containerImageName.getRegistryHost(), is("test-domain.io"));
		assertThat(containerImageName.getRepository(), is("official-repo-name/simple-repo-name"));
		assertThat(containerImageName.getCanonicalName(), is("test-domain.io/official-repo-name/simple-repo-name:tag654"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidRegistryHostName() {
		containerImageNameParser.parse("6666#.6:80/scdf/spring-image:123");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidRegistryPart() {
		containerImageNameParser.parse("localhost:80bla/scdf/spring-image:123");
	}
}
