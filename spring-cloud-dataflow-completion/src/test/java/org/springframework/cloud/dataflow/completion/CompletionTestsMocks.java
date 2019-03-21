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

package org.springframework.cloud.dataflow.completion;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * A set of mocks that consider the contents of the {@literal apps/} directory as app
 * archives.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
@Configuration
public class CompletionTestsMocks {

	private static final File ROOT = new File("src/test/resources",
			CompletionTestsMocks.class.getPackage().getName().replace('.', '/') + "/apps");

	private static final FileFilter FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory() && pathname.getName().matches(".+-.+");
		}
	};

	@Bean
	public AppRegistry appRegistry() {
		final ResourceLoader resourceLoader = new FileSystemResourceLoader();
		return new AppRegistry(new InMemoryUriRegistry(), resourceLoader) {
			@Override
			public AppRegistration find(String name, ApplicationType type) {
				String filename = name + "-" + type;
				File file = new File(ROOT, filename);
				if (file.exists()) {
					return new AppRegistration(name, type, file.toURI(), file.toURI());
				}
				else {
					return null;
				}
			}

			@Override
			public List<AppRegistration> findAll() {
				List<AppRegistration> result = new ArrayList<>();
				for (File file : ROOT.listFiles(FILTER)) {
					result.add(makeAppRegistration(file));
				}
				return result;
			}

			private AppRegistration makeAppRegistration(File file) {
				String fileName = file.getName();
				Matcher matcher = Pattern.compile("(?<name>.+)-(?<type>.+)").matcher(fileName);
				Assert.isTrue(matcher.matches(), fileName + " does not match expected pattern.");
				String name = matcher.group("name");
				ApplicationType type = ApplicationType.valueOf(matcher.group("type"));
				return new AppRegistration(name, type, file.toURI());
			}
		};
	}

	@Bean
	public ApplicationConfigurationMetadataResolver metadataResolver() {
		return new BootApplicationConfigurationMetadataResolver(
				CompletionTestsMocks.class.getClassLoader());
	}
}
