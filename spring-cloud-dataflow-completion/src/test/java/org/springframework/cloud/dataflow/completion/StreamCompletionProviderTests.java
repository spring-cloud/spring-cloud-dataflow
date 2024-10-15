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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;

import static org.assertj.core.api.Assertions.assertThat;

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
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = {CompletionConfiguration.class, CompletionTestsMocks.class}, properties = {
		"spring.main.allow-bean-definition-overriding=true"})
@SuppressWarnings("unchecked")
class StreamCompletionProviderTests {

	@Autowired
	private StreamCompletionProvider completionProvider;

	// <TAB> => file,http,etc
	@Test
	void emptyStartShouldProposeSourceOrUnboundApps() {
		assertThat(completionProvider.complete("", 1)).has(Proposals.all("orange", "http", "hdfs"));
		assertThat(completionProvider.complete("", 1)).doNotHave(Proposals.proposal("log"));
	}

	// fi<TAB> => file
	@Test
	void unfinishedAppNameShouldReturnCompletions() {
		assertThat(completionProvider.complete("h", 1)).has(Proposals.all("http", "hdfs"));
		assertThat(completionProvider.complete("ht", 1)).has(Proposals.all("http"));
		assertThat(completionProvider.complete("ht", 1)).doNotHave(Proposals.proposal("hdfs"));
	}

	@Test
	void unfinishedUnboundAppNameShouldReturnCompletions2() {
		assertThat(completionProvider.complete("", 1)).has(Proposals.all("orange"));
		assertThat(completionProvider.complete("o", 1)).has(Proposals.all("orange"));
		assertThat(completionProvider.complete("oran", 1)).has(Proposals.all("orange"));
		assertThat(completionProvider.complete("orange", 1)).has(Proposals.all("orange --expression=","orange --fooble=", "orange --expresso="));
		assertThat(completionProvider.complete("o1: orange||", 1)).has(Proposals.all("o1: orange|| orange"));
		assertThat(completionProvider.complete("o1: orange|| ", 1)).has(Proposals.all("o1: orange|| orange"));
		assertThat(completionProvider.complete("o1: orange ||", 1)).has(Proposals.all("o1: orange || orange"));
		assertThat(completionProvider.complete("o1: orange|| or", 1)).has(Proposals.all("o1: orange|| orange"));
		assertThat(completionProvider.complete("http | o", 1)).isEmpty();
		assertThat(completionProvider.complete("http|| o", 1)).has(Proposals.all("http|| orange"));
	}

	// file | filter <TAB> => file | filter | foo, etc
	@Test
	void validSubStreamDefinitionShouldReturnPipe() {
		assertThat(completionProvider.complete("http | filter ", 1)).has(Proposals.all("http | filter | log"));
		assertThat(completionProvider.complete("http | filter ", 1)).doNotHave(Proposals.proposal("http | filter | http"));
	}

	// file | filter<TAB> => file | filter --foo=, etc
	@Test
	void validSubStreamDefinitionShouldReturnAppOptions() {
		assertThat(completionProvider.complete("http | filter ", 1)).has(Proposals.all("http | filter --expression=", "http | filter --expresso="));
		// Same as above, no final space
		assertThat(completionProvider.complete("http | filter", 1)).has(Proposals.all("http | filter --expression=", "http | filter --expresso="));
	}

	// file | filter -<TAB> => file | filter --foo,etc
	@Test
	void oneDashShouldReturnTwoDashes() {
		assertThat(completionProvider.complete("http | filter -", 1)).has(Proposals.all("http | filter --expression=", "http | filter --expresso="));
	}

	// file | filter --<TAB> => file | filter --foo,etc
	@Test
	void twoDashesShouldReturnOptions() {
		assertThat(completionProvider.complete("http | filter --", 1)).has(Proposals.all("http | filter --expression=", "http | filter --expresso="));
	}

	// file |<TAB> => file | foo,etc
	@Test
	void danglingPipeShouldReturnExtraApps() {
		assertThat(completionProvider.complete("http |", 1)).has(Proposals.all("http | filter"));
		assertThat(completionProvider.complete("http | filter |", 1)).has(Proposals.all("http | filter | log", "http | filter | filter2: filter"));
	}

	// file --p<TAB> => file --preventDuplicates=, file --pattern=
	@Test
	void unfinishedOptionNameShouldComplete() {
		assertThat(completionProvider.complete("http --p", 1)).has(Proposals.all("http --port="));
	}

	// file | counter --name=foo --inputType=bar<TAB> => we're done
	@Test
	void sinkWithAllOptionsSetCantGoFurther() {
		assertThat(completionProvider.complete("http | log --port=1234 --level=debug", 1)).isEmpty();
	}

	// file | counter --name=<TAB> => nothing
	@Test
	void inGenericOptionValueCantProposeAnything() {
		assertThat(completionProvider.complete("http --port=", 1)).isEmpty();
	}

	// :foo > <TAB> ==> add app names
	@Test
	void destinationIntoApps() {
		assertThat(completionProvider.complete(":foo >", 1)).has(Proposals.all(":foo > filter", ":foo > log"));
		assertThat(completionProvider.complete(":foo >", 1)).doNotHave(Proposals.proposal(":foo > http"));
	}

	// :foo > <TAB> ==> add app names
	@Test
	void destinationIntoAppsVariant() {
		assertThat(completionProvider.complete(":foo >", 1)).has(Proposals.all(":foo > filter", ":foo > log"));
	}

	// http<TAB> (no space) => NOT "http2: http"
	@Test
	void automaticAppLabellingDoesNotGetInTheWay() {
		assertThat(completionProvider.complete("http", 1)).doNotHave(Proposals.proposal("http2: http"));
	}

	// http --use-ssl=<TAB> => propose true|false
	@Test
	void valueHintForBooleans() {
		assertThat(completionProvider.complete("http --use-ssl=", 1)).has(Proposals.all("http --use-ssl=true", "http --use-ssl=false"));
	}

	// .. foo --enum-value=<TAB> => propose enum values
	@Test
	void valueHintForEnums() {
		assertThat(completionProvider.complete("http | filter --expresso=", 1)).has(Proposals.all("http | filter --expresso=SINGLE", "http | filter --expresso=DOUBLE"));
	}

	@Test
	void unrecognizedPrefixesDontBlowUp() {
		assertThat(completionProvider.complete("foo", 1)).isEmpty();
		assertThat(completionProvider.complete("foo --", 1)).isEmpty();
		assertThat(completionProvider.complete("http --notavalidoption", 1)).isEmpty();
		assertThat(completionProvider.complete("http --notavalidoption=", 1)).isEmpty();
		assertThat(completionProvider.complete("foo --some-option", 1)).isEmpty();
		assertThat(completionProvider.complete("foo --some-option=", 1)).isEmpty();
		assertThat(completionProvider.complete("foo --some-option=prefix", 1)).isEmpty();
		assertThat(
				completionProvider.complete(
						"http | filter --port=12 --expression=something " + "--expresso=not-a-valid-prefix", 1)).isEmpty();
	}

	/*
	 * http --use-ssl=tr<TAB> => must be true or false, no need to present
	 * "...=tr --other.prop"
	 */
	@Test
	void closedSetValuesShouldBeExclusive() {
		assertThat(completionProvider.complete("http --use-ssl=tr", 1)).doNotHave(Proposals.proposal(s-> s.startsWith("http --use-ssl=tr --port")));
	}

}
