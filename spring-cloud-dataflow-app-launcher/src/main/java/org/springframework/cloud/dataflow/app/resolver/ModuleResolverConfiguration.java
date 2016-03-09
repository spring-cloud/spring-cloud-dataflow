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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Sets up the default Aether-based module resolver, unless overridden.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@EnableConfigurationProperties(MavenProperties.class)
public class ModuleResolverConfiguration {

	/**
	 * Default file path to a locally available maven repository, where modules will be downloaded.
	 */
	private static File DEFAULT_LOCAL_REPO = new File(System.getProperty("user.home") +
			File.separator + ".m2" + File.separator + "repository");

	@Autowired
	private MavenProperties properties;

	@Bean
	@ConditionalOnMissingBean(ModuleResolver.class)
	public ModuleResolver moduleResolver() {
		int i = 1;
		Map<String, String> remoteRepositoryMap = new HashMap<>();
		for (String repository : properties.getRemoteRepositories()) {
			remoteRepositoryMap.put("repository " + i++, repository);
		}
		File localRepository = (!StringUtils.isEmpty(properties.getLocalRepository()))
				? new File(properties.getLocalRepository()) : DEFAULT_LOCAL_REPO;
		AetherModuleResolver aetherModuleResolver = new AetherModuleResolver(localRepository,
				remoteRepositoryMap, properties);
		aetherModuleResolver.setOffline(properties.isOffline());
		return aetherModuleResolver;
	}
}
