/*
 * Copyright 2016-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.completion;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.completion.CompletionProposal;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.service.DefaultAppRegistryService;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@SuppressWarnings("unchecked")
class TabOnTapCompletionProviderTests {

	@Autowired
	private StreamCompletionProvider completionProvider;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private StreamDefinitionService streamDefinitionService;

	private static Condition<CompletionProposal> hasText(String text) {
		return new Condition<>(p -> Objects.equals(p.getText(), text), "text:" + text);
	}
	private static boolean hasAll(List<? extends CompletionProposal> proposals, Collection<String> items) {
		Set<String> proposalTexts = proposals.stream().map(CompletionProposal::getText).collect(Collectors.toSet());
		return items.stream().allMatch(proposalTexts::contains);
	}
	private static Condition<List<? extends CompletionProposal>> all(String ... text) {
		Set<String> items = new HashSet<>(Arrays.asList(text));
		return new Condition<>(proposals -> hasAll(proposals, items), "text:" + items);
	}

	@BeforeEach
	void setup() {
		this.streamDefinitionRepository.save(new StreamDefinition("foo", "time | transform | log"));
		this.streamDefinitionRepository.save(new StreamDefinition("bar", "time | log"));
		this.completionProvider
				.addCompletionRecoveryStrategy(new TapOnDestinationRecoveryStrategy(streamDefinitionRepository, this.streamDefinitionService));
	}

	// :foo ==> add appropriate app names
	@Test
	void appNamesAfterStreamName() {

		assertThat(completionProvider.complete(":foo", 1)).has(all(":foo.time", ":foo.transform"));
	}

	// :foo. ==> add appropriate app names
	@Test
	void appNamesAfterStreamNameWithDotAfterStreamName() {
		assertThat(completionProvider.complete(":foo.", 1)).has(all(":foo.time", ":foo.transform"));
	}

	// : ==> add stream name
	@Test
	void streamNameAfterColon() {
		assertThat(completionProvider.complete(":", 1)).has(all(":foo", ":bar"));
	}

	/**
	 * A set of mocks that consider the contents of the {@literal apps/} directory as app
	 * archives.
	 *
	 * @author Eric Bottard
	 * @author Mark Fisher
	 * @author Corneil du Plessis
	 */
	@Configuration
	public static class Mocks {

		private static final File ROOT = new File("src/test/resources/apps");

		private static final FileFilter FILTER = pathname -> pathname.isDirectory() && pathname.getName().matches(".+-.+");

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
						return new AppRegistration(name, type, file.toURI());
					}
					else {
						return null;
					}
				}

				private AppRegistration makeAppRegistration(File file) {
					String fileName = file.getName();
					Matcher matcher = Pattern.compile("(?<name>.+)-(?<type>.+)").matcher(fileName);
					Assert.isTrue(matcher.matches(), fileName + " did not match expected pattern");
					String name = matcher.group("name");
					ApplicationType type = ApplicationType.valueOf(matcher.group("type"));
					return new AppRegistration(name, type, file.toURI());
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

		@Bean
		public ApplicationConfigurationMetadataResolver configurationMetadataResolver() {
			return new BootApplicationConfigurationMetadataResolver(null, null);
		}
	}
}
