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
package org.springframework.cloud.skipper.index;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class PackageIndexDownloaderTests {

	@Autowired
	private PackageIndexDownloader packageIndexDownloader;

	@Test
	public void calculateFilename() throws IOException {
		UrlResource urlResource = new UrlResource("file:./spring-cloud-skipper-server/src/test/resources/packages.yml");
		String filename = packageIndexDownloader.computeFilename(urlResource);
		assertThat(filename).isEqualTo("file_dot_spring-cloud-skipper-server_src_test_resources_packages.yml");
		urlResource = new UrlResource(
				"file:/home/mpollack/projects/spring-cloud-skipper/spring-cloud-skipper-server/src/test/resources/packages.yml");
		filename = packageIndexDownloader.computeFilename(urlResource);
		assertThat(filename).isEqualTo(
				"file_home_mpollack_projects_spring-cloud-skipper_spring-cloud-skipper-server_src_test_resources_packages.yml");
		urlResource = new UrlResource("http://localhost:8081/packages.yml");
		filename = packageIndexDownloader.computeFilename(urlResource);
		assertThat(filename).isEqualTo("localhost_packages.yml");

		urlResource = new UrlResource("http://www.example.com/packages.yml");
		filename = packageIndexDownloader.computeFilename(urlResource);
		assertThat(filename).isEqualTo("www.example.com_packages.yml");
	}
}
