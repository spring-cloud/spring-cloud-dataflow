/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.client;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientProperties;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author Vinicius Carvalho
 */
public class DataFlowClientAutoConfigurationTests {

	@Test
	public void contextLoads() throws Exception {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(TestApplication.class,
				"--spring.cloud.dataflow.client.enableDsl=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration");
		assertThat(applicationContext.getBean(DataFlowTemplate.class)).isNotNull();
		assertThat(applicationContext.getBean(StreamBuilder.class)).isNotNull();
		RestTemplate template = applicationContext.getBean(RestTemplate.class);
		//No auth
		Mockito.verify(template, Mockito.times(0)).setRequestFactory(Mockito.any());
		applicationContext.close();
	}

	@Test
	public void usingAuthentication() throws Exception {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(TestApplication.class,
				"--spring.cloud.dataflow.client.authentication.basic.username=foo",
				"--spring.cloud.dataflow.client.authentication.basic.password=bar",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration");
		assertThat(applicationContext.getBean(DataFlowTemplate.class)).isNotNull();
		assertThat(applicationContext.getBean(StreamBuilder.class)).isNotNull();

		RestTemplate template = applicationContext.getBean(RestTemplate.class);
		DataFlowClientProperties properties = applicationContext.getBean(DataFlowClientProperties.class);
		assertThat(properties.getAuthentication()).isNotNull();
		assertThat(properties.getAuthentication().getBasic().getUsername()).isEqualTo("foo");
		assertThat(properties.getAuthentication().getBasic().getPassword()).isEqualTo("bar");
		Mockito.verify(template, Mockito.times(1)).setRequestFactory(Mockito.any());
		applicationContext.close();
	}

	@SpringBootApplication
	static class TestApplication {

		@Bean
		public RestTemplate restTemplate() {
			RestTemplate mock = Mockito.mock(RestTemplate.class);
			Mockito.when(mock.getMessageConverters()).thenReturn(Collections.singletonList(new MappingJackson2HttpMessageConverter()));
			return mock;
		}
	}
}
