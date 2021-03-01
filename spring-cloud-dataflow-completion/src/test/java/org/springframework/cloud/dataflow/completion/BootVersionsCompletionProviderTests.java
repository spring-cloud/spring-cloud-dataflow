/*
 * Copyright 2019-2021 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
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
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests that the completion mechanism knows how to cope with different versions of Spring
 * Boot, including using reflection on classes packaged in the boot archive when needed
 * (e.g. enum values completion).
 *
 * @author Eric Bottard
 * @author Christian Tzolov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { CompletionConfiguration.class,
		BootVersionsCompletionProviderTests.Mocks.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@SuppressWarnings("unchecked")
public class BootVersionsCompletionProviderTests {

	@Autowired
	private StreamCompletionProvider completionProvider;

	@Test
	public void testBoot13Layout() {
		List<CompletionProposal> result = completionProvider.complete("boot13 --", 0);
		assertThat(result, hasItems(Proposals.proposalThat(is("boot13 --level=")), Proposals.proposalThat(is("boot13 --number=")),
				Proposals.proposalThat(is("boot13 --some-string="))));

		// Test that custom classes can also be loaded correctly
		result = completionProvider.complete("boot13 --level=", 0);
		assertThat(result, hasItems(Proposals.proposalThat(is("boot13 --level=low")), Proposals.proposalThat(is("boot13 --level=high"))));

		result = completionProvider.complete("boot13 --number=", 0);
		assertThat(result, hasItems(Proposals.proposalThat(is("boot13 --number=one")), Proposals.proposalThat(is("boot13 --number=two"))));
	}

	@Test
	public void testBoot14Layout() {
		List<CompletionProposal> result = completionProvider.complete("boot14 --", 0);
		assertThat(result, hasItems(Proposals.proposalThat(is("boot14 --level=")), Proposals.proposalThat(is("boot14 --number=")),
				Proposals.proposalThat(is("boot14 --some-string="))));

		// Test that custom classes can also be loaded correctly
		result = completionProvider.complete("boot14 --level=", 0);
		assertThat(result,
				hasItems(Proposals.proposalThat(is("boot14 --level=very_low")), Proposals.proposalThat(is("boot14 --level=very_high"))));

		result = completionProvider.complete("boot14 --number=", 0);
		assertThat(result, hasItems(Proposals.proposalThat(is("boot14 --number=one")), Proposals.proposalThat(is("boot14 --number=two"))));

	}

	/**
	 * A set of mocks that consider the contents of the {@literal boot_versions/}
	 * directory as app archives.
	 *
	 * @author Eric Bottard
	 * @author Mark Fisher
	 */
	@Configuration
	public static class Mocks {

		private static final File ROOT = new File("src/test/resources",
				BootVersionsCompletionProviderTests.Mocks.class.getPackage().getName().replace('.', '/')
						+ "/boot_versions");

		@MockBean
		private DefaultContainerImageMetadataResolver containerImageMetadataResolver;

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
					result.add(find("boot13", ApplicationType.source));
					result.add(find("boot14", ApplicationType.source));
					return result;
				}

				@Override
				public AppRegistration find(String name, ApplicationType type) {
					String filename = name + "-1.0.0.BUILD-SNAPSHOT.jar";
					File file = new File(ROOT, filename);
					if (file.exists()) {
						return new AppRegistration(name, type, file.toURI(), file.toURI());
					}
					else {
						return null;
					}
				}

				@Override
				public AppRegistration save(AppRegistration app) {
					return null;
				}

				@Override
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
		public ApplicationConfigurationMetadataResolver metadataResolver() {
			return new BootApplicationConfigurationMetadataResolver(
					StreamCompletionProviderTests.class.getClassLoader(), containerImageMetadataResolver);
		}
	}
}
