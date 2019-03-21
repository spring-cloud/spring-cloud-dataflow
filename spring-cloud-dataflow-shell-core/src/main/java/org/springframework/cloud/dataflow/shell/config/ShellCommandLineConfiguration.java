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

package org.springframework.cloud.dataflow.shell.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.shell.ShellCommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the command line runner
 *
 * @author Josh Long
 * @author Mark Pollack
 */
@Configuration
public class ShellCommandLineConfiguration {

	/**
	 * Return the interactive command line runner. The {@link ConditionalOnMissingBean}
	 * annotation is used so that this interactive command line runner is not created when
	 * running the shell in the same process as the Data Flow server.
	 *
	 * @return the interactive shell
	 */
	@Bean
	@ConditionalOnMissingBean(type = "org.springframework.cloud.deployer.resource.registry.UriRegistry")
	public ShellCommandLineRunner commandLineRunner() {
		return new ShellCommandLineRunner();
	}

}
