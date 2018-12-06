/*
 * Copyright 2018 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.common.security.support.OnSecurityDisabled;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

public class OnSecurityDisabledTests {

	@Test
	public void propertySecurityEnabled() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class, "security.basic.enabled:true");
		assertThat(context.containsBean("myBean"), equalTo(false));
		context.close();
	}

	@Test
	public void propertySecurityDisabled() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class, "security.basic.enabled:false");
		assertThat(context.containsBean("myBean"), equalTo(true));
		context.close();
	}

	@Test
	public void propertySecurityMissing() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class);
		assertThat(context.containsBean("myBean"), equalTo(true));
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
	@Conditional(OnSecurityDisabled.class)
	public static class Config {
		@Bean
		public String myBean() {
			return "myBean";
		}
	}
}
