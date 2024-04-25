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
package org.springframework.cloud.dataflow.registry.support;

/**
 * @author Mark Pollack
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.params.provider.Arguments.arguments;
/**
 * Test for DockerImage parsing methods Code from https://github.com/vmware/admiral
 */

public class DockerImageTests {


	static class DockerImageNames implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
			List<Arguments> data = new ArrayList<>();
			data.add(arguments("all sections", "myhost:300/namespace/repo:tag", "myhost:300", "namespace", "repo",
					"namespace/repo", "tag"));
			data.add(arguments("repo and tag", "repo:tag", null, null, "repo", "library/repo", "tag"));
			data.add(arguments("implicit registry, repo and tag", "library/repo:tag", null, "library", "repo",
					"library/repo", "tag"));
			data.add(arguments("repo without tag", "repo", null, null, "repo", "library/repo", "latest"));
			data.add(arguments("namespace and repo", "namespace/repo", null, "namespace", "repo", "namespace/repo",
					"latest"));
			data.add(arguments("host with dot and repo", "host.name/repo", "host.name", null, "repo", "repo",
					"latest"));
			data.add(arguments("host with colon and repo", "host:3000/repo", "host:3000", null, "repo", "repo",
					"latest"));
			data.add(arguments("host with colon, repo and tag", "host:3000/repo:tag", "host:3000", null, "repo",
					"repo", "tag"));
			data.add(arguments("official repo with default namespace", "registry.hub.docker.com/library/repo:tag",
					"registry.hub.docker.com", "library", "repo", "library/repo", "tag"));
			data.add(arguments("official repo with custom namespace", "registry.hub.docker.com/user/repo:tag",
					"registry.hub.docker.com", "user", "repo", "user/repo", "tag"));
			data.add(arguments("official repo with default namespace", "docker.io/library/repo:tag", "docker.io",
					"library", "repo", "library/repo", "tag"));
			data.add(arguments("official repo with custom namespace", "docker.io/user/repo:tag", "docker.io", "user",
					"repo", "user/repo", "tag"));
			data.add(arguments("host and three path components of repo", "host/namespace/category/repo", "host",
					"namespace/category", "repo", "namespace/category/repo", "latest"));
			data.add(arguments("host, port, three path components of repo and tag",
					"host:5000/namespace/category/repo:tag", "host:5000", "namespace/category", "repo",
					"namespace/category/repo", "tag"));

			return data.stream();
		}
	}


	@ParameterizedTest
	@ArgumentsSource(DockerImageNames.class)
	public void testDockerImageParsing(String description, String fullImageName, String expectedHost, String expectedNamespace, String expectedRepo, String expectedNamespaceAndRepo, String expectedTag) {
		DockerImage dockerImage = DockerImage.fromImageName(fullImageName);
		assertEquals( expectedHost, dockerImage.getHost(), description + ": host");
		assertEquals(expectedNamespace, dockerImage.getNamespace(), description + ": namespace");
		assertEquals(expectedRepo, dockerImage.getRepository(), description + ": repository");
		assertEquals(expectedNamespaceAndRepo, dockerImage.getNamespaceAndRepo(), description + ": namespace and repo");
		assertEquals(expectedTag, dockerImage.getTag(), description + ": tag");
	}
}
