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
package org.springframework.cloud.dataflow.registry.support;

import java.net.MalformedURLException;

import org.junit.Test;

import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class ResourceUtilsTests {

	@Test(expected = IllegalArgumentException.class)
	public void testBadNamedJars() throws Exception {
		UrlResource urlResource = new UrlResource("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/1.2.0.RELEASE/file-sink-rabbit.jar");
		ResourceUtils.getUrlResourceVersion(urlResource);
	}

	public void testInvalidUrlResourceWithoutVersion() throws Exception {
		UrlResource urlResource = new UrlResource("https://repo.spring"
				+ ".io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/1.2.0.RELEASE/file-sink-rabbit-1.2.0.RELEASE.jar");
		assertThat(ResourceUtils.getUrlResourceWithoutVersion(urlResource)).isEqualTo("1.2.0.RELEASE");
	}

	@Test
	public void testInvalidURIPath() throws Exception {
		UrlResource urlResource = new UrlResource("https://com.com-0.0.2-SNAPSHOT");
		try {
			ResourceUtils.getUrlResourceVersion(urlResource);
			fail("Excepted IllegalArgumentException for an invalid URI path");
		}
		catch (Exception e) {
			assertThat(e.getMessage().equals("URI path doesn't exist"));
		}
	}

	@Test
	public void testDockerUriString() throws Exception {
		String dockerUri = "docker:springcloudstream/log-sink-rabbit:1.2.0.RELEASE";
		Resource resource = ResourceUtils.getResource(dockerUri, null);
		assertThat(resource instanceof DockerResource);
		assertThat(resource.getURI().toString().equals(dockerUri));
	}

	@Test
	public void testResourceURIWithMissingFileNameExtension() throws Exception {
		UrlResource urlResource = new UrlResource("https://com.com-0.0.2-SNAPSHOT/test");
		try {
			ResourceUtils.getUrlResourceVersion(urlResource);
			fail("Excepted IllegalArgumentException for an invalid URI path");
		}
		catch (Exception e) {
			assertThat(e.getMessage().equals("URI file name extension doesn't exist"));
		}
	}

	@Test
	public void testInvalidUrlResourceURI() throws Exception {
		UrlResource urlResource = new UrlResource("https://com.com-0.0.2-SNAPSHOT/test.zip");
		try {
			ResourceUtils.getUrlResourceVersion(urlResource);
			fail("Excepted IllegalArgumentException for an invalid URL resource URI");
		}
		catch (Exception e) {
			assertThat(e.getMessage().equals("Could not parse version from https://com.com-0.0.2-SNAPSHOT/test.zip, expected format is <artifactId>-<version>.jar"));
		}
	}

	@Test
	public void testJars() throws MalformedURLException {
		//Dashes in artifact name
		UrlResource urlResource = new UrlResource("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/file-sink-rabbit-1.2.0.RELEASE.jar");
		String version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0.RELEASE");

		String theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/file-sink-rabbit");

		//No dashes in artfiact name - BUILD-SNAPSHOT
		urlResource = new UrlResource("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file/file-1.2.0.BUILD-SNAPSHOT.jar");
		version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file/file");

		//No dashes in artfiact name - RELEASE
		urlResource = new UrlResource("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file/file-1.2.0.RELEASE.jar");
		version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0.RELEASE");
		theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file/file");

		//Spring style snapshots naming scheme
		urlResource = new UrlResource("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/file-sink-rabbit-1.2.0.BUILD-SNAPSHOT.jar");
		version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/file-sink-rabbit");

		//Standard maven style naming scheme
		urlResource = new UrlResource("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/file-sink-rabbit-1.2.0-SNAPSHOT.jar");
		version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0-SNAPSHOT");
		theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/file-sink-rabbit");
	}

	@Test
	public void testGetResource() {
		String mavenUri = "maven://org.springframework.cloud.stream.app:aggregate-counter-sink-rabbit:1.3.0.RELEASE";
		Resource resource = ResourceUtils.getResource(mavenUri, new MavenProperties());
		assertThat(resource).isInstanceOf(MavenResource.class);
	}

	@Test
	public void testGetResourceVersion() {
		String mavenUri = "maven://org.springframework.cloud.stream.app:aggregate-counter-sink-rabbit:1.3.0.RELEASE";
		String version = ResourceUtils.getResourceVersion(ResourceUtils.getResource(mavenUri, new MavenProperties()));
		assertThat(version).isEqualTo("1.3.0.RELEASE");
	}
}
