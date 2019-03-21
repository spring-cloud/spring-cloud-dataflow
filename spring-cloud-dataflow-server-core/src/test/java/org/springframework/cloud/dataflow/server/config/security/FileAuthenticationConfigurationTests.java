/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.security;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

/**
*
* @author Gunnar Hillert
*/
public class FileAuthenticationConfigurationTests {

	@Test
	public void testInitAuthenticationManagerBuilder() throws Exception {

		try {
			final FileAuthenticationConfiguration fileAuthenticationConfiguration =
					new FileAuthenticationConfiguration();
			fileAuthenticationConfiguration.init(mock(AuthenticationManagerBuilder.class));
		}
		catch (IllegalArgumentException anIllegalArgumentException) {
			assertThat(anIllegalArgumentException.getMessage(),
					is("No user specified. Please specify at least 1 user (e.g. "
							+ "via 'dataflow.security.authentication.file.users')"));
		}
	}

}
