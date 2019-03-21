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
package org.springframework.cloud.dataflow.server.config.security.support;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.security.core.GrantedAuthority;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author Gunnar Hillert
 */
public class DefaultDataflowAuthoritiesExtractorTests {

	@Test
	public void testNullMapParameter() throws Exception {
		final DefaultDataflowAuthoritiesExtractor authoritiesExtractor = new DefaultDataflowAuthoritiesExtractor();
		try {
			authoritiesExtractor.extractAuthorities(null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The map argument must not be null.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testThat3AuthoritiesAreReturned() throws Exception {
		final DefaultDataflowAuthoritiesExtractor authoritiesExtractor = new DefaultDataflowAuthoritiesExtractor();

		final List<GrantedAuthority> authorities = authoritiesExtractor.extractAuthorities(new HashMap<>());
		assertThat(authorities, hasSize(3));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW"));
	}

}
