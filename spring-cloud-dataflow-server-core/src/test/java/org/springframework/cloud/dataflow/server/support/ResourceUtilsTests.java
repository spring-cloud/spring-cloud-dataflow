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
package org.springframework.cloud.dataflow.server.support;

import org.junit.Test;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
public class ResourceUtilsTests {

	@Test(expected = IllegalArgumentException.class)
	public void testBadNamedJars() throws Exception {
		UrlResource urlResource = new UrlResource("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/1.2.0.RELEASE/file-sink-rabbit.jar");
		ResourceUtils.getUrlResourceVersion(urlResource);
	}

	@Test
	public void testJars() throws MalformedURLException {
		//Dashes in artifact name
		UrlResource urlResource = new UrlResource("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/1.2.0.RELEASE/file-sink-rabbit-1.2.0.RELEASE.jar");
		String version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0.RELEASE");

		String theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/1.2.0.RELEASE/file-sink-rabbit");

		//No dashes in artfiact name - BUILD-SNAPSHOT
		urlResource = new UrlResource("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file/1.2.0.BUILD-SNAPSHOT/file-1.2.0.BUILD-SNAPSHOT.jar");
		version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file/1.2.0.BUILD-SNAPSHOT/file");

		//No dashes in artfiact name - RELEASE
		urlResource = new UrlResource("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file/1.2.0.RELEASE/file-1.2.0.RELEASE.jar");
		version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0.RELEASE");
		theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file/1.2.0.RELEASE/file");

		//Spring style snapshots naming scheme
		urlResource = new UrlResource("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/1.2.0.RELEASE/file-sink-rabbit-1.2.0.BUILD-SNAPSHOT.jar");
		version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/1.2.0.RELEASE/file-sink-rabbit");

		//Standard maven style naming scheme
		urlResource = new UrlResource("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/1.2.0-SNAPSHOT/file-sink-rabbit-1.2.0-SNAPSHOT.jar");
		version = ResourceUtils.getUrlResourceVersion(urlResource);
		assertThat(version).isEqualTo("1.2.0-SNAPSHOT");		theRest = ResourceUtils.getResourceWithoutVersion(urlResource);
		assertThat(theRest).isEqualTo("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/file-sink-rabbit/1.2.0-SNAPSHOT/file-sink-rabbit");

	}
}
