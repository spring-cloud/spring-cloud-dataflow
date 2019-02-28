/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.config.features;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.SchedulerServiceProperties;
import org.springframework.cloud.dataflow.server.service.impl.DefaultSchedulerService;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * Establishes the {@link SchedulerService} instance to be used by SCDF.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */

@Configuration
@Conditional({SchedulerConfiguration.SchedulerConfigurationPropertyChecker.class})
@EnableConfigurationProperties({TaskConfigurationProperties.class, CommonApplicationProperties.class, SchedulerServiceProperties.class})
public class SchedulerConfiguration {

	@Value("${spring.cloud.dataflow.server.uri:}")
	private String dataflowServerUri;

	@Bean
	@ConditionalOnMissingBean
	public SchedulerService schedulerService(CommonApplicationProperties commonApplicationProperties,
											 TaskPlatform taskPlatform, TaskDefinitionRepository taskDefinitionRepository,
											 AppRegistryService registry, ResourceLoader resourceLoader,
											 TaskConfigurationProperties taskConfigurationProperties,
											 DataSourceProperties dataSourceProperties,
											 ApplicationConfigurationMetadataResolver metaDataResolver,
											 SchedulerServiceProperties schedulerServiceProperties,
											 AuditRecordService auditRecordService) {
		return new DefaultSchedulerService(commonApplicationProperties,
				taskPlatform, taskDefinitionRepository, registry, resourceLoader,
				taskConfigurationProperties, dataSourceProperties,
				this.dataflowServerUri, metaDataResolver, schedulerServiceProperties, auditRecordService);
	}

	public static class SchedulerConfigurationPropertyChecker extends AllNestedConditions {

		public SchedulerConfigurationPropertyChecker() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(prefix = FeaturesProperties.FEATURES_PREFIX, name = FeaturesProperties.SCHEDULES_ENABLED)
		static class TestSchedulerProperty {
		}

		@ConditionalOnProperty(prefix = FeaturesProperties.FEATURES_PREFIX, name = FeaturesProperties.TASKS_ENABLED, matchIfMissing = true)
		static class TestTaskProperty {
		}

	}

}
