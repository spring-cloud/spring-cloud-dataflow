/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import org.h2.tools.Server;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link H2ServerConfiguration}.
 *
 * @author Michael Wirth
 * @author Corneil du Plessis
 */
class H2ServerConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, H2ServerConfiguration.class));

	@Test
	void serverStartsWhenUrlIsH2AndEmbeddedPropertyTrue() {
		runner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=true")
				.run(assertServerStarted(19092));
	}

	@Test
	void serverStartsWhenUrlIsH2AndEmbeddedPropertyNotSet() {
		runner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow;DATABASE_TO_UPPER=FALSE")
				.run(assertServerStarted(19092));
	}

	@Test
	void serverStopsWhenContextClosed() {
		runner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow;DATABASE_TO_UPPER=FALSE")
				.run(assertServerStarted(19092))
				.run(context -> {
					Server server = context.getBean("h2TcpServer", Server.class);
					context.close();
					assertThat(server.isRunning(false)).isFalse();
				});
	}

	@Test
	void serverDoesNotStartByDefault() {
		runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("h2TcpServer"));
	}

	@Test
	void serverDoesNotStartWhenUrlIsNotH2() {
		runner.withPropertyValues(
						"spring.datasource.url=jdbc:postgresql://localhost:5432/dataflow")
				.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("h2TcpServer"));
	}

	@Test
	void serverDoesNotStartWhenEmbeddedPropertyFalse() {
		runner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=false")
				.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("h2TcpServer"));
	}

	@Test
	void serverDoesNotStartWhenUrlIsH2ButInvalidForm() {
		runner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:-1/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=true")
				.run(context -> assertThat(context)
						.getFailure().rootCause().isInstanceOf(IllegalArgumentException.class)
						.hasMessageMatching("DataSource URL .* does not match regex pattern: .*"));

		runner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:port/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=true")
				.run(context -> assertThat(context)
						.getFailure().rootCause().isInstanceOf(IllegalArgumentException.class)
						.hasMessageMatching("DataSource URL .* does not match regex pattern: .*"));

		runner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:99999/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=true")
				.run(context -> assertThat(context)
						.getFailure().rootCause().isInstanceOf(IllegalArgumentException.class)
						.hasMessage("Port value out of range: 99999"));
	}

	@Test
	void serverDoesNotStartWhenH2NotOnClasspath() {
		runner.withClassLoader(new FilteredClassLoader(Server.class)).
				withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=true")
				.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("h2TcpServer"));
	}

	private ContextConsumer<AssertableApplicationContext> assertServerStarted(int port) {
		return (context) -> {
			assertThat(context).hasNotFailed().hasBean("h2TcpServer");
			Server server = context.getBean("h2TcpServer", Server.class);
			assertThat(server.isRunning(false)).isTrue();
			assertThat(server.getPort()).isEqualTo(port);
		};
	}
}
