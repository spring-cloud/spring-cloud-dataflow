/*
 * Copyright 2015-2016 the original author or authors.
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
 * Integration tests for StreamCompletionProvider.
 *
 * <p>These tests work hand in hand with a custom {@link AppRegistry} and
 * {@link ApplicationConfigurationMetadataResolver} to provide completions for a fictional
 * set of well known apps.</p>
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CompletionConfiguration.class, StreamCompletionProviderTests.Mocks.class})
public class StreamCompletionProviderTests {

	@Autowired
	private StreamCompletionProvider completionProvider;

	@Test
	// <TAB> => file,http,etc
	public void testEmptyStartShouldProposeSourceApps() {
		assertThat(completionProvider.complete("", 1), hasItems(
				proposalThat(is("http")),
				proposalThat(is("hdfs"))
		));
		assertThat(completionProvider.complete("", 1), not(hasItems(
				proposalThat(is("log"))
		)));
	}

	@Test
	// fi<TAB> => file
	public void testUnfinishedAppNameShouldReturnCompletions() {
		assertThat(completionProvider.complete("h", 1), hasItems(
				proposalThat(is("http")),
				proposalThat(is("hdfs"))
		));
		assertThat(completionProvider.complete("ht", 1), hasItems(
				proposalThat(is("http"))
		));
		assertThat(completionProvider.complete("ht", 1), not(hasItems(
				proposalThat(is("hdfs"))
		)));
	}

	@Test
	// file | filter <TAB> => file | filter | foo, etc
	public void testValidSubStreamDefinitionShouldReturnPipe() {
		assertThat(completionProvider.complete("http | filter ", 1), hasItems(
				proposalThat(is("http | filter | log"))
		));
		assertThat(completionProvider.complete("http | filter ", 1), not(hasItems(
				proposalThat(is("http | filter | http"))
		)));
	}

	@Test
	// file | filter<TAB> => file | filter --foo=, etc
	public void testValidSubStreamDefinitionShouldReturnAppOptions() {
		assertThat(completionProvider.complete("http | filter ", 1), hasItems(
				proposalThat(is("http | filter --expression=")),
				proposalThat(is("http | filter --expresso="))
		));
		// Same as above, no final space
		assertThat(completionProvider.complete("http | filter", 1), hasItems(
				proposalThat(is("http | filter --expression=")),
				proposalThat(is("http | filter --expresso="))
		));
	}

	@Test
	// file | filter -<TAB> => file | filter --foo,etc
	public void testOneDashShouldReturnTwoDashes() {
		assertThat(completionProvider.complete("http | filter -", 1), hasItems(
				proposalThat(is("http | filter --expression=")),
				proposalThat(is("http | filter --expresso="))
		));
	}

	@Test
	// file | filter --<TAB> => file | filter --foo,etc
	public void testTwoDashesShouldReturnOptions() {
		assertThat(completionProvider.complete("http | filter --", 1), hasItems(
				proposalThat(is("http | filter --expression=")),
				proposalThat(is("http | filter --expresso="))
		));
	}

	@Test
	// file |<TAB> => file | foo,etc
	public void testDanglingPipeShouldReturnExtraApps() {
		assertThat(completionProvider.complete("http |", 1), hasItems(
				proposalThat(is("http | filter"))
		));
		assertThat(completionProvider.complete("http | filter |", 1), hasItems(
				proposalThat(is("http | filter | log")),
				proposalThat(is("http | filter | filter2: filter"))
		));
	}

	@Test
	// file --p<TAB> => file --preventDuplicates=, file --pattern=
	public void testUnfinishedOptionNameShouldComplete() {
		assertThat(completionProvider.complete("http --p", 1), hasItems(
				proposalThat(is("http --port="))
		));
	}

	@Test
	// file | counter --name=foo --inputType=bar<TAB> => we're done
	public void testSinkWithAllOptionsSetCantGoFurther() {
		assertThat(completionProvider.complete("http | log --port=1234 --level=debug", 1), empty());
	}

	@Test
	// file | counter --name=<TAB> => nothing
	public void testInGenericOptionValueCantProposeAnything() {
		assertThat(completionProvider.complete("http --port=", 1), empty());
	}

	@Test
	// :foo > <TAB>  ==> add app names
	public void testDestinationIntoApps() {
		assertThat(completionProvider.complete(":foo >", 1), hasItems(
				proposalThat(is(":foo > filter")),
				proposalThat(is(":foo > log"))
		));
		assertThat(completionProvider.complete(":foo >", 1), not(hasItems(
				proposalThat(is(":foo > http"))
		)));
	}

	@Test
	// :foo > <TAB>  ==> add app names
	public void testDestinationIntoAppsVariant() {
		assertThat(completionProvider.complete(":foo >", 1), hasItems(
				proposalThat(is(":foo > filter")),
				proposalThat(is(":foo > log"))
		));
	}

	@Test
	// http<TAB> (no space) => NOT "http2: http"
	public void testAutomaticAppLabellingDoesNotGetInTheWay() {
		assertThat(completionProvider.complete("http", 1), not(hasItems(
				proposalThat(is("http2: http"))
		)));
	}

	@Test
	// http --use-ssl=<TAB> => propose true|false
	public void testValueHintForBooleans() {
		assertThat(completionProvider.complete("http --use-ssl=", 1), hasItems(
				proposalThat(is("http --use-ssl=true")),
				proposalThat(is("http --use-ssl=false"))
		));
	}

	@Test
	// .. foo --enum-value=<TAB> => propose enum values
	public void testValueHintForEnums() {
		assertThat(completionProvider.complete("http | filter --expresso=", 1), hasItems(
				proposalThat(is("http | filter --expresso=SINGLE")),
				proposalThat(is("http | filter --expresso=DOUBLE"))
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
		assertThat(completionProvider.complete("http | filter --port=12 --expression=something --expresso=not-a-valid-prefix", 1), empty());
	}

	/*
	 * http --use-ssl=tr<TAB> => must be true or false, no need to present "...=tr --other.prop"
	 */
	@Test
	public void testClosedSetValuesShouldBeExclusive() {
		assertThat(completionProvider.complete("http --use-ssl=tr", 1), not(hasItems(
				proposalThat(startsWith("http --use-ssl=tr --port"))
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
					return new AppRegistration(name, type, file.toURI(), resourceLoader);
				}
			};
		}

		@Bean
		public ApplicationConfigurationMetadataResolver metadataResolver() {
			return new BootApplicationConfigurationMetadataResolver(StreamCompletionProviderTests.class.getClassLoader());
		}
	}

}
