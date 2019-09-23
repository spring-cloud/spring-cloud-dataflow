/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.scheduler.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.domain.Dependency;
import org.springframework.cloud.skipper.domain.VersionInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.CollectionModel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mark Pollack
 * @author Glenn Renfro
 */

@EnableAutoConfiguration(exclude = {DataFlowClientAutoConfiguration.class})
@EnableDataFlowServer
@Configuration
public class TestConfig {

	@Bean
	public Scheduler localScheduler() {
		// This is in auto-config package and we can depend on that, use same
		// dummy no-op impl here.
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

	@Bean
	@Primary
	public SkipperClient skipperClientMock() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		AboutResource about = new AboutResource();
		about.setVersionInfo(new VersionInfo());
		about.getVersionInfo().setServer(new Dependency());
		about.getVersionInfo().getServer().setName("Test Server");
		about.getVersionInfo().getServer().setVersion("Test Version");
		when(skipperClient.info()).thenReturn(about);
		when(skipperClient.listDeployers()).thenReturn(new CollectionModel<>(new ArrayList<>(), new ArrayList<>()));
		return skipperClient;
	}

}
