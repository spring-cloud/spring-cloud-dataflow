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
package org.springframework.cloud.dataflow.registry;

import java.io.File;
import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Oleg Zhurakousky
 *
 */
public class AppRegistryTests {

	private AppRegistry appRegistry;

	@Before
	public void before(){
		UriRegistry uriRegistry = mock(UriRegistry.class);
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		this.appRegistry = new AppRegistry(uriRegistry, resourceLoader);
	}

	@Test
	public void testWithMavenURI() throws Exception {
		URI uri = new URI("maven://com.example:mytask:1.0.2");
		AppRegistration registration = this.appRegistry.save("foo", ApplicationType.processor, uri, null);
		assertThat(registration != null);
		assertEquals("foo", registration.getName());
		assertEquals(ApplicationType.processor, registration.getType());
		assertEquals(uri, registration.getUri());
	}

	@Test
	public void testWithExistingJARFileURI() throws Exception {
		File jar = new File("foo.jar");
		jar.createNewFile();
		jar.deleteOnExit();

		URI uri = jar.toURI();
		AppRegistration registration = this.appRegistry.save("foo", ApplicationType.processor, uri, null);
		assertThat(registration != null);
		assertEquals("foo", registration.getName());
		assertEquals(ApplicationType.processor, registration.getType());
		assertEquals(uri, registration.getUri());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithNonExistingJARFileURI() throws Exception {
		URI uri = new URI("file:/bar.jar");
		this.appRegistry.save("foo", ApplicationType.processor, uri, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithNonExistingDirNotJarURI() throws Exception {
		File dir = new File("foo");
		dir.mkdir();
		dir.deleteOnExit();

		URI uri = dir.toURI();
		this.appRegistry.save("foo", ApplicationType.processor, uri, null);
	}
}
