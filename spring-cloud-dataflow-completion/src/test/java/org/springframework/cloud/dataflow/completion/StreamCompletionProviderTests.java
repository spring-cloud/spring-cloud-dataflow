/*
 * Copyright 2015-2018 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for StreamCompletionProvider.
 * <p>
 * <p>
 * These tests work hand in hand with a custom {@link org.springframework.cloud.dataflow.registry.service.AppRegistryService} and
 * {@link ApplicationConfigurationMetadataResolver} to provide completions for a fictional
 * set of well known apps.
 * </p>
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { CompletionConfiguration.class, CompletionTestsMocks.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@SuppressWarnings("unchecked")
public class StreamCompletionProviderTests {

	@Autowired
	private StreamCompletionProvider completionProvider;

	@Test
	// <TAB> => file,http,etc
	public void testEmptyStartShouldProposeSourceOrUnboundApps() {
		assertThat(completionProvider.complete("", 1), hasItems(Proposals.proposalThat(is("orange")),
			Proposals.proposalThat(is("http")), Proposals.proposalThat(is("hdfs"))));
		assertThat(completionProvider.complete("", 1), not(hasItems(Proposals.proposalThat(is("log")))));
	}

	@Test
	// fi<TAB> => file
	public void testUnfinishedAppNameShouldReturnCompletions() {
		assertThat(completionProvider.complete("h", 1), hasItems(Proposals.proposalThat(is("http")), Proposals.proposalThat(is("hdfs"))));
		assertThat(completionProvider.complete("ht", 1), hasItems(Proposals.proposalThat(is("http"))));
		assertThat(completionProvider.complete("ht", 1), not(hasItems(Proposals.proposalThat(is("hdfs")))));
	}

	@Test
	public void testUnfinishedUnboundAppNameShouldReturnCompletions2() {
		assertThat(completionProvider.complete("", 1), hasItems(Proposals.proposalThat(is("orange"))));
		assertThat(completionProvider.complete("o", 1), hasItems(Proposals.proposalThat(is("orange"))));
		assertThat(completionProvider.complete("oran", 1), hasItems(Proposals.proposalThat(is("orange"))));
		assertThat(completionProvider.complete("orange", 1), hasItems(Proposals.proposalThat(is("orange --expression=")),
				Proposals.proposalThat(is("orange --fooble=")),Proposals.proposalThat(is("orange --expresso="))));
		assertThat(completionProvider.complete("o1: orange||", 1), hasItems(Proposals.proposalThat(is("o1: orange|| orange"))));
		assertThat(completionProvider.complete("o1: orange|| ", 1), hasItems(Proposals.proposalThat(is("o1: orange|| orange"))));
		assertThat(completionProvider.complete("o1: orange ||", 1), hasItems(Proposals.proposalThat(is("o1: orange || orange"))));
		assertThat(completionProvider.complete("o1: orange|| or", 1), hasItems(Proposals.proposalThat(is("o1: orange|| orange"))));
		assertThat(completionProvider.complete("http | o", 1), empty());
		assertThat(completionProvider.complete("http|| o", 1), hasItems(Proposals.proposalThat(is("http|| orange"))));
	}

	@Test
	// file | filter <TAB> => file | filter | foo, etc
	public void testValidSubStreamDefinitionShouldReturnPipe() {
		assertThat(completionProvider.complete("http | filter ", 1), hasItems(Proposals.proposalThat(is("http | filter | log"))));
		assertThat(completionProvider.complete("http | filter ", 1),
				not(hasItems(Proposals.proposalThat(is("http | filter | http")))));
	}

	@Test
	// file | filter<TAB> => file | filter --foo=, etc
	public void testValidSubStreamDefinitionShouldReturnAppOptions() {
		assertThat(completionProvider.complete("http | filter ", 1), hasItems(
				Proposals.proposalThat(is("http | filter --expression=")), Proposals.proposalThat(is("http | filter --expresso="))));
		// Same as above, no final space
		assertThat(completionProvider.complete("http | filter", 1), hasItems(
				Proposals.proposalThat(is("http | filter --expression=")), Proposals.proposalThat(is("http | filter --expresso="))));
	}

	@Test
	// file | filter -<TAB> => file | filter --foo,etc
	public void testOneDashShouldReturnTwoDashes() {
		assertThat(completionProvider.complete("http | filter -", 1), hasItems(
				Proposals.proposalThat(is("http | filter --expression=")), Proposals.proposalThat(is("http | filter --expresso="))));
	}

	@Test
	// file | filter --<TAB> => file | filter --foo,etc
	public void testTwoDashesShouldReturnOptions() {
		assertThat(completionProvider.complete("http | filter --", 1), hasItems(
				Proposals.proposalThat(is("http | filter --expression=")), Proposals.proposalThat(is("http | filter --expresso="))));
	}

	@Test
	// file |<TAB> => file | foo,etc
	public void testDanglingPipeShouldReturnExtraApps() {
		assertThat(completionProvider.complete("http |", 1), hasItems(Proposals.proposalThat(is("http | filter"))));
		assertThat(completionProvider.complete("http | filter |", 1),
				hasItems(Proposals.proposalThat(is("http | filter | log")), Proposals.proposalThat(is("http | filter | filter2: filter"))));
	}

	@Test
	// file --p<TAB> => file --preventDuplicates=, file --pattern=
	public void testUnfinishedOptionNameShouldComplete() {
		assertThat(completionProvider.complete("http --p", 1), hasItems(Proposals.proposalThat(is("http --port="))));
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
	// :foo > <TAB> ==> add app names
	public void testDestinationIntoApps() {
		assertThat(completionProvider.complete(":foo >", 1),
				hasItems(Proposals.proposalThat(is(":foo > filter")), Proposals.proposalThat(is(":foo > log"))));
		assertThat(completionProvider.complete(":foo >", 1), not(hasItems(Proposals.proposalThat(is(":foo > http")))));
	}

	@Test
	// :foo > <TAB> ==> add app names
	public void testDestinationIntoAppsVariant() {
		assertThat(completionProvider.complete(":foo >", 1),
				hasItems(Proposals.proposalThat(is(":foo > filter")), Proposals.proposalThat(is(":foo > log"))));
	}

	@Test
	// http<TAB> (no space) => NOT "http2: http"
	public void testAutomaticAppLabellingDoesNotGetInTheWay() {
		assertThat(completionProvider.complete("http", 1), not(hasItems(Proposals.proposalThat(is("http2: http")))));
	}

	@Test
	// http --use-ssl=<TAB> => propose true|false
	public void testValueHintForBooleans() {
		assertThat(completionProvider.complete("http --use-ssl=", 1),
				hasItems(Proposals.proposalThat(is("http --use-ssl=true")), Proposals.proposalThat(is("http --use-ssl=false"))));
	}

	@Test
	// .. foo --enum-value=<TAB> => propose enum values
	public void testValueHintForEnums() {
		assertThat(completionProvider.complete("http | filter --expresso=", 1),
				hasItems(Proposals.proposalThat(is("http | filter --expresso=SINGLE")),
						Proposals.proposalThat(is("http | filter --expresso=DOUBLE"))));
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
		assertThat(
				completionProvider.complete(
						"http | filter --port=12 --expression=something " + "--expresso=not-a-valid-prefix", 1),
				empty());
	}

	/*
	 * http --use-ssl=tr<TAB> => must be true or false, no need to present
	 * "...=tr --other.prop"
	 */
	@Test
	public void testClosedSetValuesShouldBeExclusive() {
		assertThat(completionProvider.complete("http --use-ssl=tr", 1),
				not(hasItems(Proposals.proposalThat(startsWith("http --use-ssl=tr --port")))));
	}

}
