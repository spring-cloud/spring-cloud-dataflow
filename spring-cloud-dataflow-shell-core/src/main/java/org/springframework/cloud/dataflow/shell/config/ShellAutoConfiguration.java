/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.config;

import org.jline.terminal.Terminal;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.shell.ShellProperties;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.ConfigCommands;
import org.springframework.cloud.dataflow.shell.command.support.ShellUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.boot.NonInteractiveShellRunnerCustomizer;
import org.springframework.shell.result.ThrowableResultHandler;
import org.springframework.web.client.RestTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Dataflow Shell components.
 *
 * @author Josh Long
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Chris Bono
 */
@AutoConfiguration
@EnableConfigurationProperties({
		ShellProperties.class,
		DataFlowShellProperties.class
})
public class ShellAutoConfiguration {

	@Bean
	public TargetHolder targetHolder() {
		return new TargetHolder();
	}

	@Bean
	public RestTemplate restTemplate() {
		return DataFlowTemplate.getDefaultDataflowRestTemplate();
	}

	@Bean
	public TablesInfoResultHandler tablesInfoResultHandler(Terminal terminal) {
		return new TablesInfoResultHandler(terminal);
	}

	@Configuration(proxyBeanMethods = false)
	static class ShellRunnerConfiguration {

		@Bean
		public ApplicationRunner helpAwareApplicationRunner() {
			return new HelpApplicationRunner();
		}

		@Bean
		public ApplicationRunner initializeConnectionApplicationRunner(TargetHolder targetHolder, ConfigCommands configCommands,
				ThrowableResultHandler resultHandler) {
			return new InitializeConnectionApplicationRunner(targetHolder, configCommands, resultHandler);
		}

		@Bean
		public NonInteractiveShellRunnerCustomizer dataFlowCommandFilesArgsCustomizer(ShellProperties shellProperties) {
			return (shellRunner) -> shellRunner.setCommandsFromInputArgs((args) ->
					ShellUtils.filteredArgsToShellCommands(shellProperties, args));
		}
	}

}
