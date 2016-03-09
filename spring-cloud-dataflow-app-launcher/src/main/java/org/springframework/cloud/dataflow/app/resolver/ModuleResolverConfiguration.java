/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.app.resolver;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Sets up the default Aether-based module resolver, unless overridden.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@EnableConfigurationProperties({ModuleResolverProperties.class, AetherProperties.class})
public class ModuleResolverConfiguration {

	@Autowired
	private ModuleResolverProperties properties;

	@Autowired
	private AetherProperties proxyProperties;

	@Bean
	@ConditionalOnMissingBean(ModuleResolver.class)
	public ModuleResolver moduleResolver() {
		int i = 1;
		Map<String, String> repositoriesMap = new HashMap<>();
		for (String repository : properties.getRemoteRepositories()) {
			repositoriesMap.put("repository " + i++, repository);
		}
		AetherModuleResolver aetherModuleResolver = new AetherModuleResolver(properties.getLocalRepository(),
				repositoriesMap, proxyProperties);
		aetherModuleResolver.setOffline(properties.isOffline());
		return aetherModuleResolver;
	}
}
