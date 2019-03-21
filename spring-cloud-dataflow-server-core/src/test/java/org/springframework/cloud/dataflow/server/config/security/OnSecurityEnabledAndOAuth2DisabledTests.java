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

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.dataflow.server.config.security.support.OnSecurityEnabledAndOAuth2Disabled;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Gunnar Hillert
 */
public class OnSecurityEnabledAndOAuth2DisabledTests {

	@Test
	public void noPropertySet() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class);
		assertThat(context.containsBean("myBean"), equalTo(false));
		context.close();
	}

	@Test
	public void basicSecurityEnabled() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class, "security.basic.enabled:true");
		assertThat(context.containsBean("myBean"), equalTo(true));
		context.close();
	}

	@Test
	public void basicSecurityDefaultAndOauth2Enabled() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class, "security.oauth2.client.client-id:12345");
		assertThat(context.containsBean("myBean"), equalTo(false));
		context.close();
	}

	@Test
	public void basicSecurityEnabledAndOauth2Enabled() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class, "security.basic.enabled:true",
				"security" + ".oauth2.client.client-id:12345");
		assertThat(context.containsBean("myBean"), equalTo(false));
		context.close();
	}

	private AnnotationConfigApplicationContext load(Class<?> config, String... env) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, env);
		context.register(config);
		context.refresh();
		return context;
	}

	@Configuration
	@Conditional(OnSecurityEnabledAndOAuth2Disabled.class)
	public static class Config {
		@Bean
		public String myBean() {
			return "myBean";
		}
	}
}
