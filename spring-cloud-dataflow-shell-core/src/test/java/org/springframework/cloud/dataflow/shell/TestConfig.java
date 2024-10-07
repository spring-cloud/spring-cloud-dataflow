/*
 * Copyright 2018-2022 the original author or authors.
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
package org.springframework.cloud.dataflow.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.domain.Dependency;
import org.springframework.cloud.skipper.domain.VersionInfo;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.Shell;
import org.springframework.shell.ShellRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mark Pollack
 * @author Chris Bono
 */
@SpringBootApplication(exclude = {DataFlowClientAutoConfiguration.class, SimpleTaskAutoConfiguration.class})
@EnableDataFlowServer
@Configuration
public class TestConfig {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	ShellRunner shortCircuitShellRunner() {
		return new ShortCircuitShellRunner();
	}

	/**
	 * A shell runner that handles all commands (first in the runner chain because it has highest precedence order)
	 * by simply dropping the command - effectively short-circuiting the default shell runners.
	 *
	 * <p>The whole purpose of this is to allow the app to load all Shell related components but not actually handle
	 * any incoming commands. This is needed because the integration tests use the {@link ShellCommandRunner} which in
	 * turn provides convenience methods that allow the tests to invoke the {@link Shell#evaluate} method directly,
	 * thus avoiding the need to have shell runners also attempting to handle commands. In fact, if the interactive
	 * runner attempts to handle the commands, it will block for user input and hang the tests.
	 */
	static class ShortCircuitShellRunner implements ShellRunner, Ordered {
		@Override
		public boolean canRun(ApplicationArguments args) {
			return true;
		}

		@Override
		public void run(ApplicationArguments args) throws Exception {
			System.out.println("Short-circuited command : " + args);
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}
	}

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
		when(skipperClient.listDeployers()).thenReturn(new ArrayList<>());
		return skipperClient;
	}

	@Bean
	@ConditionalOnMissingBean({ActuatorOperations.class})
	ActuatorOperations actuatorOperations() {
		return mock(ActuatorOperations.class);
	}

}
