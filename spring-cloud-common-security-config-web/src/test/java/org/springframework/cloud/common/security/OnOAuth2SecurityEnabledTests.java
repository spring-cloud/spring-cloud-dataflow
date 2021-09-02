/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.cloud.common.security;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.common.security.support.OnOAuth2SecurityEnabled;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Gunnar Hillert
 */
public class OnOAuth2SecurityEnabledTests {

	@Test
	public void noPropertySet() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class);
		assertThat(context.containsBean("myBean")).isFalse();
		context.close();
	}

	@Test
	public void propertySecurityOauth() throws Exception {
		assertThatThrownBy(() -> {
			load(Config.class, "spring.security.oauth2");
		}).isInstanceOf(IllegalStateException.class);		
	}

	@Test
	public void propertyClientId() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class,
				"spring.security.oauth2.client.registration.uaa.client-id:12345");
		assertThat(context.containsBean("myBean")).isTrue();
		context.close();
	}

	@Test
	public void clientIdOnlyWithNoValue() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class,
				"spring.security.oauth2.client.registration.uaa.client-id");
		assertThat(context.containsBean("myBean")).isTrue();
		context.close();
	}

	private AnnotationConfigApplicationContext load(Class<?> config, String... env) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(env).applyTo(context);
		context.register(config);
		context.refresh();
		return context;
	}

	@Configuration
	@Conditional(OnOAuth2SecurityEnabled.class)
	public static class Config {
		@Bean
		public String myBean() {
			return "myBean";
		}
	}
}
