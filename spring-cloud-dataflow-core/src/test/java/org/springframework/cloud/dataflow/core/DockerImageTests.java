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
package org.springframework.cloud.dataflow.core;

/**
 * @author Mark Pollack
 */

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Test for DockerImage parsing methods Code from https://github.com/vmware/admiral
 */
@RunWith(Parameterized.class)
public class DockerImageTests {
	private final String description;

	private final String fullImageName;

	private final String expectedHost;

	private final String expectedNamespace;

	private final String expectedRepo;

	private final String expectedNamespaceAndRepo;

	private final String expectedTag;

	/**
	 * @param expectedHost
	 * @param expectedNamespace
	 * @param expectedRepo
	 */
	public DockerImageTests(String description, String fullImageName, String expectedHost,
			String expectedNamespace,
			String expectedRepo,
			String expectedNamespaceAndRepo,
			String expectedTag) {

		this.description = description;
		this.fullImageName = fullImageName;
		this.expectedHost = expectedHost;
		this.expectedNamespace = expectedNamespace;
		this.expectedRepo = expectedRepo;
		this.expectedNamespaceAndRepo = expectedNamespaceAndRepo;
		this.expectedTag = expectedTag;
	}

	@Parameterized.Parameters
	public static List<String[]> data() {
		List<String[]> data = new ArrayList<>();
		data.add(new String[] { "all sections", "myhost:300/namespace/repo:tag", "myhost:300",
				"namespace", "repo", "namespace/repo", "tag" });

		data.add(new String[] { "repo and tag", "repo:tag", null, null, "repo", "library/repo",
				"tag" });

		data.add(new String[] { "implicit registry, repo and tag", "library/repo:tag", null,
				"library", "repo", "library/repo", "tag" });

		data.add(new String[] { "repo without tag", "repo", null, null, "repo", "library/repo",
				"latest" });

		data.add(new String[] { "namespace and repo", "namespace/repo", null, "namespace", "repo",
				"namespace/repo", "latest" });

		data.add(new String[] { "host with dot and repo", "host.name/repo", "host.name", null,
				"repo", "repo", "latest" });

		data.add(new String[] { "host with colon and repo", "host:3000/repo", "host:3000", null,
				"repo", "repo", "latest" });

		data.add(new String[] { "host with colon, repo and tag", "host:3000/repo:tag", "host:3000",
				null, "repo", "repo", "tag" });

		data.add(new String[] { "official repo with default namespace",
				"registry.hub.docker.com/library/repo:tag", "registry.hub.docker.com", "library",
				"repo", "library/repo", "tag" });

		data.add(new String[] { "official repo with custom namespace",
				"registry.hub.docker.com/user/repo:tag", "registry.hub.docker.com", "user", "repo",
				"user/repo", "tag" });

		data.add(new String[] { "official repo with default namespace",
				"docker.io/library/repo:tag", "docker.io", "library", "repo", "library/repo",
				"tag" });

		data.add(new String[] { "official repo with custom namespace",
				"docker.io/user/repo:tag", "docker.io", "user", "repo", "user/repo", "tag" });

		data.add(new String[] { "host and three path components of repo",
				"host/namespace/category/repo", "host", "namespace/category", "repo",
				"namespace/category/repo", "latest" });

		data.add(new String[] { "host, port, three path components of repo and tag",
				"host:5000/namespace/category/repo:tag", "host:5000", "namespace/category", "repo",
				"namespace/category/repo", "tag" });

		return data;
	}

	@Test
	public void testDockerImageParsing() {

		DockerImage dockerImage = DockerImage.fromImageName(fullImageName);
		assertEquals(description + ": host", expectedHost, dockerImage.getHost());
		assertEquals(description + ": namespace", expectedNamespace, dockerImage.getNamespace());
		assertEquals(description + ": repository", expectedRepo, dockerImage.getRepository());
		assertEquals(description + ": namespace and repo", expectedNamespaceAndRepo,
				dockerImage.getNamespaceAndRepo());
		assertEquals(description + ": tag", expectedTag, dockerImage.getTag());
	}
}
