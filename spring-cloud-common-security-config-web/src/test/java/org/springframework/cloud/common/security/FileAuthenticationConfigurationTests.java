/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.common.security;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.common.security.support.FileSecurityProperties;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gunnar Hillert
 */
@SpringBootTest(classes = FileAuthenticationConfigurationTests.FileAuthConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
public class FileAuthenticationConfigurationTests {

	@Autowired
	private FileAuthenticationConfiguration fileAuthenticationConfiguration;

	@Test
	public void testInitAuthenticationManagerBuilder() throws Exception {

		try {
			this.fileAuthenticationConfiguration.init(mock(AuthenticationManagerBuilder.class));
		}
		catch (IllegalArgumentException anIllegalArgumentException) {
			assertThat(anIllegalArgumentException.getMessage(),
					is("No user specified. Please specify at least 1 user for the file based authentication."));
		}
	}

	@Configuration
	public static class FileAuthConfiguration {

		@Bean
		public FileSecurityProperties fileSecurityProperties() {
			return new FileSecurityProperties();
		}

		@Bean
		public FileAuthenticationConfiguration fileAuthenticationConfiguration() {
			return new FileAuthenticationConfiguration();
		}
	}

}
