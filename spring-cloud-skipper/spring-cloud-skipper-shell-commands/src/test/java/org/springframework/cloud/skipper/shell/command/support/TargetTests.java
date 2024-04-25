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
package org.springframework.cloud.skipper.shell.command.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
public class TargetTests {

	@Test
	public void constructorTests() {
		Target target = new Target("http://localhost:7577", "username", "password", true);
		assertThat(target.getTargetUri()).hasPort(7577)
				.hasPath("")
				.hasHost("localhost");
		assertThat(target.isSkipSslValidation()).isEqualTo(true);
		assertThat(target.getTargetCredentials()).isEqualTo(new TargetCredentials("username", "password"));
		target = new Target("http://localhost:7577");
		assertThat(target.getTargetCredentials()).isNull();

	}

	@Test
	public void testStatus() {
		Target target = new Target("http://localhost:7577", "username", "password", true);
		assertThat(target.getStatus()).isNull();
		target.setTargetException(new IllegalArgumentException("This is bad"));
		assertThat(target.getStatus()).isEqualTo(Target.TargetStatus.ERROR);

	}
}
