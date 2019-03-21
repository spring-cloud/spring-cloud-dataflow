/*
 * Copyright 2016 the original author or authors.
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.dataflow.completion.Proposals.proposalThat;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

/**
 * Integration tests for TaskCompletionProvider.
 *
 * <p>These tests work hand in hand with a custom {@link AppRegistry} and
 * {@link ApplicationConfigurationMetadataResolver} to provide completions for a fictional
 * set of well known apps.</p>
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Andy Clement
 */
@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CompletionConfiguration.class, TaskCompletionProviderTests.Mocks.class})
public class TaskCompletionProviderTests {

	@Autowired
	private TaskCompletionProvider completionProvider;

	@Test
	// <TAB> => basic,plum,etc
	public void testEmptyStartShouldProposeSourceApps() {
		assertThat(completionProvider.complete("", 1), hasItems(
				proposalThat(is("basic")),
				proposalThat(is("plum"))
		));
		assertThat(completionProvider.complete("", 1), not(hasItems(
				proposalThat(is("log"))
		)));
	}

	@Test
	// b<TAB> => basic
	public void testUnfinishedAppNameShouldReturnCompletions() {
		assertThat(completionProvider.complete("b", 1), hasItems(
				proposalThat(is("basic"))
		));
		assertThat(completionProvider.complete("ba", 1), hasItems(
				proposalThat(is("basic"))
		));
		assertThat(completionProvider.complete("pl", 1), not(hasItems(
				proposalThat(is("basic"))
		)));
	}

	@Test
	// basic<TAB> => basic --foo=, etc
	public void testValidTaskDefinitionShouldReturnAppOptions() {
		assertThat(completionProvider.complete("basic ", 1), hasItems(
				proposalThat(is("basic --expression=")),
				proposalThat(is("basic --expresso="))
		));
		// Same as above, no final space
		assertThat(completionProvider.complete("basic", 1), hasItems(
				proposalThat(is("basic --expression=")),
				proposalThat(is("basic --expresso="))
		));
	}

	@Test
	// file | filter -<TAB> => file | filter --foo,etc
	public void testOneDashShouldReturnTwoDashes() {
		assertThat(completionProvider.complete("basic -", 1), hasItems(
				proposalThat(is("basic --expression=")),
				proposalThat(is("basic --expresso="))
		));
	}

	@Test
	// basic --<TAB> => basic --foo,etc
	public void testTwoDashesShouldReturnOptions() {
		assertThat(completionProvider.complete("basic --", 1), hasItems(
				proposalThat(is("basic --expression=")),
				proposalThat(is("basic --expresso="))
		));
	}

	@Test
	// file --p<TAB> => file --preventDuplicates=, file --pattern=
	public void testUnfinishedOptionNameShouldComplete() {
		assertThat(completionProvider.complete("basic --foo", 1), hasItems(
				proposalThat(is("basic --fooble="))
		));
	}

	@Test
	// file | counter --name=<TAB> => nothing
	public void testInGenericOptionValueCantProposeAnything() {
		assertThat(completionProvider.complete("basic --expression=", 1), empty());
	}

	@Test
	// plum --use-ssl=<TAB> => propose true|false
	public void testValueHintForBooleans() {
		assertThat(completionProvider.complete("plum --use-ssl=", 1), hasItems(
				proposalThat(is("plum --use-ssl=true")),
				proposalThat(is("plum --use-ssl=false"))
		));
	}

	@Test
	// basic --enum-value=<TAB> => propose enum values
	public void testValueHintForEnums() {
		assertThat(completionProvider.complete("basic --expresso=", 1), hasItems(
				proposalThat(is("basic --expresso=SINGLE")),
				proposalThat(is("basic --expresso=DOUBLE"))
		));
	}

	@Test
	public void testUnrecognizedPrefixesDontBlowUp() {
		assertThat(completionProvider.complete("foo", 1), empty());
		assertThat(completionProvider.complete("foo --", 1), empty());
		assertThat(completionProvider.complete("http --notavalidoption", 1), empty());
		assertThat(completionProvider.complete("http --notavalidoption=", 1), empty());
		assertThat(completionProvider.complete("foo --some-option", 1), empty());
		assertThat(completionProvider.complete("foo --some-option=", 1), empty());
		assertThat(completionProvider.complete("foo --some-option=prefix", 1), empty());
	}

	/*
	 * basic --expresso=s<TAB> => must be single or double, no need to present "--expresso=s --other.prop"
	 */
	@Test
	public void testClosedSetValuesShouldBeExclusive() {
		assertThat(completionProvider.complete("basic --expresso=s", 1), not(hasItems(
				proposalThat(startsWith("basic --expresso=s --fooble"))
		)));
	}

	/**
	 * A set of mocks that consider the contents of the {@literal apps/} directory as app
	 * archives.
	 *
	 * @author Eric Bottard
	 * @author Mark Fisher
	 */
	@Configuration
	public static class Mocks {

		private static final File ROOT = new File("src/test/resources", Mocks.class.getPackage().getName().replace('.', '/') + "/apps");

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
						return new AppRegistration(name, type, file.toURI(), resourceLoader);
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
					Assert.isTrue(matcher.matches());
					String name = matcher.group("name");
					ApplicationType type = ApplicationType.valueOf(matcher.group("type"));
					System.out.println("makeAppRegistration "+fileName);
					return new AppRegistration(name, type, file.toURI(), resourceLoader);
				}
			};
		}

		@Bean
		public ApplicationConfigurationMetadataResolver metadataResolver() {
			return new BootApplicationConfigurationMetadataResolver(TaskCompletionProviderTests.class.getClassLoader());
		}
	}

}
