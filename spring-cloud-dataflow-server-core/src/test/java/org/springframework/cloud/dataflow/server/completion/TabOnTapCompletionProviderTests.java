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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.FeatureMatcher;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@SuppressWarnings("unchecked")
public class TabOnTapCompletionProviderTests {

	@Autowired
	private StreamCompletionProvider completionProvider;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private StreamDefinitionService streamDefinitionService;

	private static org.hamcrest.Matcher<CompletionProposal> proposalThat(org.hamcrest.Matcher<String> matcher) {
		return new FeatureMatcher<CompletionProposal, String>(matcher, "a proposal whose text", "text") {
			@Override
			protected String featureValueOf(CompletionProposal actual) {
				return actual.getText();
			}
		};
	}

	@BeforeEach
	public void setup() {
		this.streamDefinitionRepository.save(new StreamDefinition("foo", "time | transform | log"));
		this.streamDefinitionRepository.save(new StreamDefinition("bar", "time | log"));
		this.completionProvider
				.addCompletionRecoveryStrategy(new TapOnDestinationRecoveryStrategy(streamDefinitionRepository, this.streamDefinitionService));
	}

	@Test
	// :foo ==> add appropriate app names
	public void testAppNamesAfterStreamName() {
		assertThat(completionProvider.complete(":foo", 1),
				hasItems(proposalThat(is(":foo.time")), proposalThat(is(":foo.transform"))));
	}

	@Test
	// :foo. ==> add appropriate app names
	public void testAppNamesAfterStreamNameWithDotAfterStreamName() {
		assertThat(completionProvider.complete(":foo.", 1),
				hasItems(proposalThat(is(":foo.time")), proposalThat(is(":foo.transform"))));
	}

	@Test
	// : ==> add stream name
	public void testStreamNameAfterColon() {
		assertThat(completionProvider.complete(":", 1), hasItems(proposalThat(is(":foo")), proposalThat(is(":bar"))));
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
