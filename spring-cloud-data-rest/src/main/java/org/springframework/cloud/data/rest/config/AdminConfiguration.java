/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.rest.config;

import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

import java.util.List;

import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.cloud.data.module.deployer.local.LocalModuleDeployer;
import org.springframework.cloud.data.module.registry.ModuleRegistry;
import org.springframework.cloud.data.module.registry.StubModuleRegistry;
import org.springframework.cloud.data.rest.repository.InMemoryStreamDefinitionRepository;
import org.springframework.cloud.data.rest.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.data.rest.repository.StreamDefinitionRepository;
import org.springframework.cloud.data.rest.repository.TaskDefinitionRepository;
import org.springframework.cloud.stream.module.launcher.ModuleLauncher;
import org.springframework.cloud.stream.module.launcher.ModuleLauncherConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Configuration for admin application context. This includes support
 * for various runtime profiles (local, cloud, etc) and REST API framework
 * configuration.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Patrick Peralta
 * @author Thomas Risberg
 * @author Janne Valkealahti
 */
@Configuration
@EnableHypermediaSupport(type = HAL)
@EnableSpringDataWebSupport
@Import({ CloudConfiguration.class, YarnConfiguration.class })
@ComponentScan(basePackages = "org.springframework.cloud.data.rest.controller")
public class AdminConfiguration {

	@Bean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return new InMemoryStreamDefinitionRepository();
	}

	@Bean
	public TaskDefinitionRepository taskDefinitionRepository() {
		return new InMemoryTaskDefinitionRepository();
	}

	@Bean
	public ModuleRegistry moduleRegistry() {
		return new StubModuleRegistry();
	}

	@Configuration
	@Conditional(LocalCondition.class)
	@Import(ModuleLauncherConfiguration.class)
	protected static class LocalConfig {

		@Bean
		public ModuleDeployer moduleDeployer(ModuleLauncher moduleLauncher) {
			return new LocalModuleDeployer(moduleLauncher);
		}
	}

	@Bean
	public WebMvcConfigurer configurer() {
		return new WebMvcConfigurerAdapter() {

			@Override
			public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
				// the REST API produces JSON only; adding this converter
				// prevents the registration of the default converters
				converters.add(new MappingJackson2HttpMessageConverter());
			}
		};
	}

}
