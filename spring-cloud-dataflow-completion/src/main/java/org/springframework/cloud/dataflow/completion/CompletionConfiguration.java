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

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolverAutoConfiguration;
import org.springframework.cloud.dataflow.core.DefaultStreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Include this Configuration class to expose fully configured
 * {@link StreamCompletionProvider} and {@link TaskCompletionProvider}.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@Configuration
@Import({ ApplicationConfigurationMetadataResolverAutoConfiguration.class })
public class CompletionConfiguration {

	@Autowired
	private AppRegistryService appRegistry;

	@Autowired
	private ApplicationConfigurationMetadataResolver metadataResolver;

	@Bean
	@ConditionalOnMissingBean
	public StreamDefinitionService streamDefinitionService() {
		return new DefaultStreamDefinitionService();
	}

	@Bean
	public StreamCompletionProvider streamCompletionProvider(StreamDefinitionService streamDefinitionService) {
		List<RecoveryStrategy<?>> recoveryStrategies = Arrays.asList(
				emptyStartYieldsAppsRecoveryStrategy(streamDefinitionService),
				expandOneDashToTwoDashesRecoveryStrategy(streamDefinitionService),
				configurationPropertyNameAfterDashDashRecoveryStrategy(streamDefinitionService),
				unfinishedConfigurationPropertyNameRecoveryStrategy(streamDefinitionService),
				destinationNameYieldsAppsRecoveryStrategy(streamDefinitionService),
				appsAfterPipeRecoveryStrategy(streamDefinitionService), appsAfterDoublePipeRecoveryStrategy(streamDefinitionService),
				configurationPropertyValueHintRecoveryStrategy(streamDefinitionService));
		List<ExpansionStrategy> expansionStrategies = Arrays.asList(addAppOptionsExpansionStrategy(streamDefinitionService),
				pipeIntoOtherAppsExpansionStrategy(streamDefinitionService), unfinishedAppNameExpansionStrategy(streamDefinitionService),
				// Make sure this one runs last, as it may clear already computed
				// proposals
				// and return its own as the sole candidates
				configurationPropertyValueHintExpansionStrategy(streamDefinitionService));

		return new StreamCompletionProvider(recoveryStrategies, expansionStrategies, streamDefinitionService);
	}

	@Bean
	public RecoveryStrategy<?> emptyStartYieldsAppsRecoveryStrategy(StreamDefinitionService streamDefinitionService) {
		return new EmptyStartYieldsSourceOrUnboundAppsRecoveryStrategy(appRegistry, streamDefinitionService());
	}

	@Bean
	public RecoveryStrategy<?> expandOneDashToTwoDashesRecoveryStrategy(StreamDefinitionService streamDefinitionService) {
		return new ExpandOneDashToTwoDashesRecoveryStrategy(streamDefinitionService);
	}

	@Bean
	public ConfigurationPropertyNameAfterDashDashRecoveryStrategy configurationPropertyNameAfterDashDashRecoveryStrategy(StreamDefinitionService streamDefinitionService) {
		return new ConfigurationPropertyNameAfterDashDashRecoveryStrategy(appRegistry, metadataResolver, streamDefinitionService);
	}

	@Bean
	public RecoveryStrategy<?> unfinishedConfigurationPropertyNameRecoveryStrategy(StreamDefinitionService streamDefinitionService) {
		return new UnfinishedConfigurationPropertyNameRecoveryStrategy(appRegistry, metadataResolver, streamDefinitionService);
	}

	@Bean
	public RecoveryStrategy<?> appsAfterPipeRecoveryStrategy(StreamDefinitionService streamDefinitionService) {
		return new AppsAfterPipeRecoveryStrategy(appRegistry, streamDefinitionService);
	}

	@Bean
	public RecoveryStrategy<?> appsAfterDoublePipeRecoveryStrategy(StreamDefinitionService streamDefinitionService) {
		return new AppsAfterDoublePipeRecoveryStrategy(appRegistry, streamDefinitionService);
	}

	@Bean
	public RecoveryStrategy<?> destinationNameYieldsAppsRecoveryStrategy(StreamDefinitionService streamDefinitionService) {
		return new DestinationNameYieldsAppsRecoveryStrategy(appRegistry, streamDefinitionService);
	}

	@Bean
	public RecoveryStrategy<?> configurationPropertyValueHintRecoveryStrategy(StreamDefinitionService streamDefinitionService) {
		return new ConfigurationPropertyValueHintRecoveryStrategy(appRegistry, metadataResolver, streamDefinitionService);
	}

	@Bean
	public ExpansionStrategy addAppOptionsExpansionStrategy(StreamDefinitionService streamDefinitionService) {
		return new AddAppOptionsExpansionStrategy(appRegistry, metadataResolver, streamDefinitionService);
	}

	@Bean
	public ExpansionStrategy unfinishedAppNameExpansionStrategy(StreamDefinitionService streamDefinitionService) {
		return new UnfinishedAppNameExpansionStrategy(appRegistry, streamDefinitionService);
	}

	@Bean
	public ExpansionStrategy pipeIntoOtherAppsExpansionStrategy(StreamDefinitionService streamDefinitionService) {
		return new PipeIntoOtherAppsExpansionStrategy(appRegistry, streamDefinitionService);
	}

	@Bean
	public ExpansionStrategy configurationPropertyValueHintExpansionStrategy(StreamDefinitionService streamDefinitionService) {
		return new ConfigurationPropertyValueHintExpansionStrategy(appRegistry, metadataResolver, streamDefinitionService);
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

	@Bean
	public TaskCompletionProvider taskCompletionProvider() {
		List<RecoveryStrategy<?>> recoveryStrategies = Arrays.<RecoveryStrategy<?>>asList(
				emptyStartYieldsAppsTaskRecoveryStrategy(), expandOneDashToTwoDashesTaskRecoveryStrategy(),
				configurationPropertyNameAfterDashDashTaskRecoveryStrategy(),
				unfinishedConfigurationPropertyNameTaskRecoveryStrategy(),
				configurationPropertyValueHintTaskRecoveryStrategy());
		List<TaskExpansionStrategy> expansionStrategies = Arrays.asList(addTaskAppOptionsExpansionStrategy(),
				unfinishedTaskAppNameExpansionStrategy(),
				// Make sure this one runs last, as it may clear already computed
				// proposals
				// and return its own as the sole candidates
				taskConfigurationPropertyValueHintExpansionStrategy());

		return new TaskCompletionProvider(recoveryStrategies, expansionStrategies);
	}

	@Bean
	public RecoveryStrategy<?> emptyStartYieldsAppsTaskRecoveryStrategy() {
		return new EmptyStartYieldsSourceAppsTaskRecoveryStrategy(appRegistry);
	}

	@Bean
	public TaskExpansionStrategy addTaskAppOptionsExpansionStrategy() {
		return new AddAppOptionsTaskExpansionStrategy(appRegistry, metadataResolver);
	}

	@Bean
	public TaskExpansionStrategy unfinishedTaskAppNameExpansionStrategy() {
		return new UnfinishedTaskAppNameExpansionStrategy(appRegistry);
	}

	@Bean
	public TaskExpansionStrategy taskConfigurationPropertyValueHintExpansionStrategy() {
		return new ConfigurationPropertyValueHintTaskExpansionStrategy(appRegistry, metadataResolver);
	}

	@Bean
	public RecoveryStrategy<?> expandOneDashToTwoDashesTaskRecoveryStrategy() {
		return new ExpandOneDashToTwoDashesTaskRecoveryStrategy();
	}

	@Bean
	public ConfigurationPropertyNameAfterDashDashTaskRecoveryStrategy configurationPropertyNameAfterDashDashTaskRecoveryStrategy() {
		return new ConfigurationPropertyNameAfterDashDashTaskRecoveryStrategy(appRegistry, metadataResolver);
	}

	@Bean
	public RecoveryStrategy<?> configurationPropertyValueHintTaskRecoveryStrategy() {
		return new ConfigurationPropertyValueHintTaskRecoveryStrategy(appRegistry, metadataResolver);
	}

	@Bean
	public RecoveryStrategy<?> unfinishedConfigurationPropertyNameTaskRecoveryStrategy() {
		return new UnfinishedConfigurationPropertyNameTaskRecoveryStrategy(appRegistry, metadataResolver);
	}
}
