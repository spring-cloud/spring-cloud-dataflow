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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TaskCompletionProvider.
 * <p>
 * <p>
 * These tests work hand in hand with a custom {@link org.springframework.cloud.dataflow.registry.service.AppRegistryService} and
 * {@link ApplicationConfigurationMetadataResolver} to provide completions for a fictional
 * set of well known apps.
 * </p>
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Andy Clement
 * @author Corneil du Plessis
 */
@SuppressWarnings("unchecked")
@SpringBootTest(classes = {CompletionConfiguration.class, CompletionTestsMocks.class}, properties = {
		"spring.main.allow-bean-definition-overriding=true"})
class TaskCompletionProviderTests {

	@Autowired
	private TaskCompletionProvider completionProvider;

	// <TAB> => basic,plum,etc
	@Test
	void emptyStartShouldProposeSourceApps() {
		assertThat(completionProvider.complete("", 1)).has(Proposals.all("basic", "plum"));
		assertThat(completionProvider.complete("", 1)).doNotHave(Proposals.proposal("log"));
	}

	// b<TAB> => basic
	@Test
	void unfinishedAppNameShouldReturnCompletions() {
		assertThat(completionProvider.complete("b", 1)).has(Proposals.all("basic"));
		assertThat(completionProvider.complete("ba", 1)).has(Proposals.all("basic"));
		assertThat(completionProvider.complete("pl", 1)).doNotHave(Proposals.proposal("basic"));
	}

	// basic<TAB> => basic --foo=, etc
	@Test
	void validTaskDefinitionShouldReturnAppOptions() {
		assertThat(completionProvider.complete("basic ", 1))
				.has(Proposals.all("basic --expression=", "basic --expresso="));
		// Same as above, no final space
		assertThat(completionProvider.complete("basic", 1))
				.has(Proposals.all("basic --expression=", "basic --expresso="));
	}

	// file | filter -<TAB> => file | filter --foo,etc
	@Test
	void oneDashShouldReturnTwoDashes() {
		assertThat(completionProvider.complete("basic -", 1))
				.has(Proposals.all("basic --expression=", "basic --expresso="));
	}

	// basic --<TAB> => basic --foo,etc
	@Test
	void twoDashesShouldReturnOptions() {
		assertThat(completionProvider.complete("basic --", 1))
				.has(Proposals.all("basic --expression=", "basic --expresso="));
	}

	// file --p<TAB> => file --preventDuplicates=, file --pattern=
	@Test
	void unfinishedOptionNameShouldComplete() {
		assertThat(completionProvider.complete("basic --foo", 1)).has(Proposals.all("basic --fooble="));
	}

	// file | counter --name=<TAB> => nothing
	@Test
	void inGenericOptionValueCantProposeAnything() {
		assertThat(completionProvider.complete("basic --expression=", 1)).isEmpty();
	}

	// plum --use-ssl=<TAB> => propose true|false
	@Test
	void valueHintForBooleans() {
		assertThat(completionProvider.complete("plum --use-ssl=", 1))
				.has(Proposals.all("plum --use-ssl=true", "plum --use-ssl=false"));
	}

	// basic --enum-value=<TAB> => propose enum values
	@Test
	void valueHintForEnums() {
		assertThat(completionProvider.complete("basic --expresso=", 1))
				.has(Proposals.all("basic --expresso=SINGLE", "basic --expresso=DOUBLE"));
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
	}

	/*
	 * basic --expresso=s<TAB> => must be single or double, no need to present
	 * "--expresso=s --other.prop"
	 */
	@Test
	void closedSetValuesShouldBeExclusive() {
		assertThat(completionProvider.complete("basic --expresso=s", 1))
				.doNotHave(Proposals.proposal(s -> s.startsWith("basic --expresso=s --fooble")));
	}
}
