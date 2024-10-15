/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.autoconfigure.local;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.server.config.OnLocalPlatform;
import org.springframework.cloud.dataflow.server.config.features.SchedulerConfiguration;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * @author Mark Pollack
 */
@AutoConfiguration
@Conditional({OnLocalPlatform.class, SchedulerConfiguration.SchedulerConfigurationPropertyChecker.class})
public class LocalSchedulerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Scheduler localScheduler() {
		return new Scheduler() {
			@Override
			public void schedule(ScheduleRequest scheduleRequest) {
				throw new UnsupportedOperationException("Scheduling is not implemented for local platform.");
			}

			@Override
			public void unschedule(String scheduleName) {
				throw new UnsupportedOperationException("Scheduling is not implemented for local platform.");
			}

			@Override
			public List<ScheduleInfo> list(String taskDefinitionName) {
				return Collections.emptyList();
			}

			@Override
			public List<ScheduleInfo> list() {
				return Collections.emptyList();
			}
		};
	}
}
