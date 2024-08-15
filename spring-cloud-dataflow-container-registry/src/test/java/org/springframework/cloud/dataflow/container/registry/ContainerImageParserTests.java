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

package org.springframework.cloud.dataflow.container.registry;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
class ContainerImageParserTests {

	private ContainerImageParser containerImageNameParser =
			new ContainerImageParser("test-domain.io", "tag654", "official-repo-name");


	@Test
	void parseWithoutDefaults2() {
		ContainerImage containerImageName =
				containerImageNameParser.parse("dev.registry.tanzu.vmware.com/p-scdf-for-kubernetes/spring-cloud-dataflow-composed-task-runner@sha256:c838be82e886b0db98ed847487ec6bf94f12e511ebe5659bd5fbe43597a4b734");

		assertThat(containerImageName.getHostname()).isEqualTo("dev.registry.tanzu.vmware.com");
		assertThat(containerImageName.getRepositoryNamespace()).isEqualTo("p-scdf-for-kubernetes");
		assertThat(containerImageName.getRepositoryName()).isEqualTo("spring-cloud-dataflow-composed-task-runner");
		assertThat(containerImageName.getRepositoryTag()).isNull();
		assertThat(containerImageName.getRepositoryReference()).isEqualTo("sha256:c838be82e886b0db98ed847487ec6bf94f12e511ebe5659bd5fbe43597a4b734");
		assertThat(containerImageName.getRepositoryDigest()).isEqualTo("sha256:c838be82e886b0db98ed847487ec6bf94f12e511ebe5659bd5fbe43597a4b734");
		assertThat(containerImageName.getRepositoryReferenceType()).isEqualTo(ContainerImage.RepositoryReferenceType.digest);

		assertThat(containerImageName.getRegistryHost()).isEqualTo("dev.registry.tanzu.vmware.com");
		assertThat(containerImageName.getRepository()).isEqualTo("p-scdf-for-kubernetes/spring-cloud-dataflow-composed-task-runner");

		assertThat(containerImageName.getCanonicalName()).isEqualTo("dev.registry.tanzu.vmware.com/p-scdf-for-kubernetes/spring-cloud-dataflow-composed-task-runner@sha256:c838be82e886b0db98ed847487ec6bf94f12e511ebe5659bd5fbe43597a4b734");
	}

	@Test
	void parseWithoutDefaults() {
		ContainerImage containerImageName =
				containerImageNameParser.parse("springsource-docker-private-local.jfrog.io:80/scdf/stream/spring-cloud-dataflow-acceptance-image-drivers173:123");

		assertThat(containerImageName.getHostname()).isEqualTo("springsource-docker-private-local.jfrog.io");
		assertThat(containerImageName.getPort()).isEqualTo("80");
		assertThat(containerImageName.getRepositoryNamespace()).isEqualTo("scdf/stream");
		assertThat(containerImageName.getRepositoryName()).isEqualTo("spring-cloud-dataflow-acceptance-image-drivers173");
		assertThat(containerImageName.getRepositoryTag()).isEqualTo("123");
		assertThat(containerImageName.getRepositoryReference()).isEqualTo("123");
		assertThat(containerImageName.getRepositoryReferenceType()).isEqualTo(ContainerImage.RepositoryReferenceType.tag);

		assertThat(containerImageName.getRegistryHost()).isEqualTo("springsource-docker-private-local.jfrog.io:80");
		assertThat(containerImageName.getRepository()).isEqualTo("scdf/stream/spring-cloud-dataflow-acceptance-image-drivers173");

		assertThat(containerImageName.getCanonicalName()).isEqualTo("springsource-docker-private-local.jfrog.io:80/scdf/stream/spring-cloud-dataflow-acceptance-image-drivers173:123");
	}

	@Test
	void parseWithoutDigest() {
		ContainerImage containerImageName =
				containerImageNameParser.parse("springsource-docker-private-local.jfrog.io:80/scdf/stream/spring-cloud-dataflow-acceptance-image-drivers173@sha256:d44e9ac4c4bf53fb0b5424c35c85230a28eb03f24a2ade5bb7f2cc1462846401");

		assertThat(containerImageName.getHostname()).isEqualTo("springsource-docker-private-local.jfrog.io");
		assertThat(containerImageName.getPort()).isEqualTo("80");
		assertThat(containerImageName.getRepositoryNamespace()).isEqualTo("scdf/stream");
		assertThat(containerImageName.getRepositoryName()).isEqualTo("spring-cloud-dataflow-acceptance-image-drivers173");
		assertThat(containerImageName.getRepositoryDigest()).isEqualTo("sha256:d44e9ac4c4bf53fb0b5424c35c85230a28eb03f24a2ade5bb7f2cc1462846401");
		assertThat(containerImageName.getRepositoryReference()).isEqualTo("sha256:d44e9ac4c4bf53fb0b5424c35c85230a28eb03f24a2ade5bb7f2cc1462846401");
		assertThat(containerImageName.getRepositoryReferenceType()).isEqualTo(ContainerImage.RepositoryReferenceType.digest);

		assertThat(containerImageName.getRegistryHost()).isEqualTo("springsource-docker-private-local.jfrog.io:80");
		assertThat(containerImageName.getRepository()).isEqualTo("scdf/stream/spring-cloud-dataflow-acceptance-image-drivers173");

		assertThat(containerImageName.getCanonicalName()).isEqualTo("springsource-docker-private-local.jfrog.io:80/scdf/stream/spring-cloud-dataflow-acceptance-image-drivers173@sha256:d44e9ac4c4bf53fb0b5424c35c85230a28eb03f24a2ade5bb7f2cc1462846401");
	}

	@Test
	void parseWithDefaults() {
		ContainerImage containerImageName = containerImageNameParser.parse("simple-repo-name");

		assertThat(containerImageName.getHostname()).isEqualTo("test-domain.io");
		assertThat(containerImageName.getPort()).isNull();
		assertThat(containerImageName.getRepositoryNamespace()).isEqualTo("official-repo-name");
		assertThat(containerImageName.getRepositoryName()).isEqualTo("simple-repo-name");
		assertThat(containerImageName.getRepositoryTag()).isEqualTo("tag654");

		assertThat(containerImageName.getRegistryHost()).isEqualTo("test-domain.io");
		assertThat(containerImageName.getRepository()).isEqualTo("official-repo-name/simple-repo-name");
		assertThat(containerImageName.getCanonicalName()).isEqualTo("test-domain.io/official-repo-name/simple-repo-name:tag654");
	}

	@Test
	void invalidRegistryHostName() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
				containerImageNameParser.parse("6666#.6:80/scdf/spring-image:123"));
	}

	@Test
	void invalidRegistryPart() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
				containerImageNameParser.parse("localhost:80bla/scdf/spring-image:123"));
	}
}
