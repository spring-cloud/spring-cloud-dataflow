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

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolverAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Include this Configuration class to expose a fully configured {@link StreamCompletionProvider}.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@Configuration
@Import({ApplicationConfigurationMetadataResolverAutoConfiguration.class})
public class CompletionConfiguration {

	@Autowired
	private AppRegistry appRegistry;

	@Autowired
	private ApplicationConfigurationMetadataResolver metadataResolver;

	@Bean
	public StreamCompletionProvider streamCompletionProvider() {
		List<RecoveryStrategy<?>> recoveryStrategies = Arrays.<RecoveryStrategy<?>>asList(
				emptyStartYieldsAppsRecoveryStrategy(),
				expandOneDashToTwoDashesRecoveryStrategy(),
				configurationPropertyNameAfterDashDashRecoveryStrategy(),
				unfinishedConfigurationPropertyNameRecoveryStrategy(),
				destinationNameYieldsAppsRecoveryStrategy(),
				appsAfterPipeRecoveryStrategy(),
				configurationPropertyValueHintRecoveryStrategy()
		);
		List<ExpansionStrategy> expansionStrategies = Arrays.asList(
				addAppOptionsExpansionStrategy(),
				pipeIntoOtherAppsExpansionStrategy(),
				unfinishedAppNameExpansionStrategy(),
				// Make sure this one runs last, as it may clear already computed proposals
				// and return its own as the sole candidates
				configurationPropertyValueHintExpansionStrategy()
		);

		return new StreamCompletionProvider(recoveryStrategies, expansionStrategies);
	}

	@Bean
	public RecoveryStrategy<?> emptyStartYieldsAppsRecoveryStrategy() {
		return new EmptyStartYieldsSourceAppsRecoveryStrategy(appRegistry);
	}

	@Bean
	public RecoveryStrategy<?> expandOneDashToTwoDashesRecoveryStrategy() {
		return new ExpandOneDashToTwoDashesRecoveryStrategy();
	}

	@Bean
	public ConfigurationPropertyNameAfterDashDashRecoveryStrategy configurationPropertyNameAfterDashDashRecoveryStrategy() {
		return new ConfigurationPropertyNameAfterDashDashRecoveryStrategy(appRegistry, metadataResolver);
	}

	@Bean
	public RecoveryStrategy<?> unfinishedConfigurationPropertyNameRecoveryStrategy() {
		return new UnfinishedConfigurationPropertyNameRecoveryStrategy(appRegistry, metadataResolver);
	}

	@Bean
	public RecoveryStrategy<?> appsAfterPipeRecoveryStrategy() {
		return new AppsAfterPipeRecoveryStrategy(appRegistry);
	}

	@Bean
	public RecoveryStrategy<?> destinationNameYieldsAppsRecoveryStrategy() {
		return new DestinationNameYieldsAppsRecoveryStrategy(appRegistry);
	}

	@Bean
	public RecoveryStrategy<?> configurationPropertyValueHintRecoveryStrategy() {
		return new ConfigurationPropertyValueHintRecoveryStrategy(appRegistry, metadataResolver);
	}

	@Bean
	public ExpansionStrategy addAppOptionsExpansionStrategy() {
		return new AddAppOptionsExpansionStrategy(appRegistry, metadataResolver);
	}

	@Bean
	public ExpansionStrategy unfinishedAppNameExpansionStrategy() {
		return new UnfinishedAppNameExpansionStrategy(appRegistry);
	}

	@Bean
	public ExpansionStrategy pipeIntoOtherAppsExpansionStrategy() {
		return new PipeIntoOtherAppsExpansionStrategy(appRegistry);
	}

	@Bean
	public ExpansionStrategy configurationPropertyValueHintExpansionStrategy() {
		return new ConfigurationPropertyValueHintExpansionStrategy(appRegistry, metadataResolver);
	}

	@Bean
	public ValueHintProvider defaultValueHintProvider() {
		return new DefaultValueHintProvider();
	}

	@Bean
	public ValueHintProvider enumValueHintProvider() {
		return new EnumValueHintProvider();
	}

	@Bean
	public ValueHintProvider booleanValueHintProvider() {
		return new BooleanValueHintProvider();
	}
}
