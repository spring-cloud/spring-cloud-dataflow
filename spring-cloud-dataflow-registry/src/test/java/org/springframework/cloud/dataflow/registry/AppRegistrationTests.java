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
import java.io.IOException;
import java.net.URI;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertThat;
import static org.springframework.cloud.dataflow.core.ApplicationType.task;

/**
 * Unit tests for {@link AppRegistration}.
 *
 * @author Eric Bottard
 */
public class AppRegistrationTests {

	@Test
	public void testResource() {
		AppRegistration registration = new AppRegistration("foo", task, URI.create("file:///foobar"), new DefaultResourceLoader());
		assertThat(registration.getResource(), Matchers.is(Matchers.notNullValue(Resource.class)));
	}

	@Test
	public void testMetadata() throws IOException {
		AppRegistration registration = new AppRegistration("foo", task, URI.create("file:///foobar"), new DefaultResourceLoader());
		assertThat(registration.getMetadataResource().getFile(), Matchers.is(new File("/foobar")));

		registration = new AppRegistration("foo", task, URI.create("file:///foobar"), URI.create("file:///foobar-metadata"), new DefaultResourceLoader());
		assertThat(registration.getMetadataResource().getFile(), Matchers.is(new File("/foobar-metadata")));
	}

}
