/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.shell;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
@ExtendWith(SpringExtension.class)
// Avoids calling 'run' on the ShellCommandLineRunner
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = CommandLineRunner.class))
public class ShellApplicationTests {

	@Test
	public void contextLoads() {

	}

	/**
	 * Provide ApplicationArguments since Boot will not provide them in this test
	 * configuration.
	 */
	@Configuration
	static class ShellTestConfiguration {

		@Bean
		public ApplicationArguments applicationArguments() {
			return new ApplicationArguments() {
				@Override
				public String[] getSourceArgs() {
					return new String[0];
				}

				@Override
				public Set<String> getOptionNames() {
					return null;
				}

				@Override
				public boolean containsOption(String name) {
					return false;
				}

				@Override
				public List<String> getOptionValues(String name) {
					return null;
				}

				@Override
				public List<String> getNonOptionArgs() {
					return null;
				}
			};
		}
	}

}
