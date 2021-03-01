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

package org.springframework.cloud.dataflow.completion;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.container.DefaultContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryService;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.DefaultStreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.service.DefaultAppRegistryService;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.util.Assert;

import static org.mockito.Mockito.mock;

/**
 * A set of mocks that consider the contents of the {@literal apps/} directory as app
 * archives.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Christian Tzolov
 */
@Configuration
public class CompletionTestsMocks {

	private static final File ROOT = new File("src/test/resources",
			CompletionTestsMocks.class.getPackage().getName().replace('.', '/') + "/apps");

	private static final FileFilter FILTER = pathname -> pathname.isDirectory() && pathname.getName().matches(".+-.+");

	@Bean
	@ConditionalOnMissingBean
	public StreamDefinitionService streamDefinitionService() {
		return new DefaultStreamDefinitionService();
	}

	@Bean
	public AppRegistryService appRegistry() {

		return new DefaultAppRegistryService(mock(AppRegistrationRepository.class),
				new AppResourceCommon(new MavenProperties(), new FileSystemResourceLoader()),
				mock(DefaultAuditRecordService.class)) {

			@Override
			public boolean appExist(String name, ApplicationType type) {
				return false;
			}

			@Override
			public List<AppRegistration> findAll() {
				List<AppRegistration> result = new ArrayList<>();
				for (File file : ROOT.listFiles(FILTER)) {
					result.add(makeAppRegistration(file));
				}
				return result;
			}

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

			private AppRegistration makeAppRegistration(File file) {
				String fileName = file.getName();
				Matcher matcher = Pattern.compile("(?<name>.+)-(?<type>.+)").matcher(fileName);
				Assert.isTrue(matcher.matches(), fileName + " does not match expected pattern.");
				String name = matcher.group("name");
				ApplicationType type = ApplicationType.valueOf(matcher.group("type"));
				return new AppRegistration(name, type, file.toURI());
			}

			@Override
			public AppRegistration save(AppRegistration app) {
				return null;
			}

			protected boolean isOverwrite(AppRegistration app, boolean overwrite) {
				return false;
			}
		};
	}

	@MockBean
	ContainerRegistryService containerRegistryService;

	@Bean
	public ContainerImageMetadataResolver containerImageMetadataResolver(ContainerRegistryService containerRegistryService) {
		return new DefaultContainerImageMetadataResolver(containerRegistryService);
	}

	@Bean
	public ApplicationConfigurationMetadataResolver metadataResolver(ContainerImageMetadataResolver containerImageMetadataResolver) {
		return new BootApplicationConfigurationMetadataResolver(
				CompletionTestsMocks.class.getClassLoader(), containerImageMetadataResolver);
	}
}
