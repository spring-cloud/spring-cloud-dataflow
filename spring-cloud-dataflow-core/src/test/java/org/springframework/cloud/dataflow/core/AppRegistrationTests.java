/*
 * Copyright 2017 the original author or authors.
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

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AppRegistration}.
 *
 * @author Eric Bottard
 */
public class AppRegistrationTests {

	// @Test
	// public void testResource() {
	// AppRegistration registration = new AppRegistration("foo", task,
	// URI.create("file:///foobar"),
	// new DefaultResourceLoader());
	// assertThat(registration.getResource()).isNotNull();
	// }

	// @Test
	// public void testMetadata() throws IOException {
	// AppRegistration registration = new AppRegistration("foo", task,
	// URI.create("file:///foobar"),
	// URI.create("file:///foobar"), new DefaultResourceLoader());
	// assertThat(registration.getMetadataResource().getFile()).hasName("foobar");
	//
	// registration = new AppRegistration("foo", task, URI.create("file:///foobar"),
	// new DefaultResourceLoader());
	// assertThat(registration.getMetadataResource()).isNull();
	//
	// registration = new AppRegistration("foo", task, URI.create("file:///foobar"),
	// URI.create("file:///foobar-metadata"), new DefaultResourceLoader());
	// assertThat(registration.getMetadataResource().getFile()).hasName("foobar-metadata");
	// }

	@Test
	public void testCompareTo() {
		AppRegistration registration1 = new AppRegistration("foo", ApplicationType.task, URI.create("file:///foobar"));
		AppRegistration registration2 = new AppRegistration("foo2", ApplicationType.task, URI.create("file:///foobar2"));
		assertThat(registration1).isNotEqualByComparingTo(registration2);
		AppRegistration registration3 = new AppRegistration("foo1", ApplicationType.task, URI.create("file:///foobar"));
		assertThat(registration1).isNotEqualByComparingTo(registration3);
		AppRegistration registration4 = new AppRegistration("foo", ApplicationType.task, URI.create("file:///foobar"));
		assertThat(registration1).isEqualByComparingTo(registration4);
	}

	@Test
	public void testToString() {
		AppRegistration registration1 = new AppRegistration("foo", ApplicationType.task, URI.create("file:///foobar"),
				URI.create("file:///foobar-metadata"));
		assertThat(registration1.toString()).contains("foo").contains("task").contains("file:///foobar")
				.contains("file:///foobar-metadata");
	}

}
