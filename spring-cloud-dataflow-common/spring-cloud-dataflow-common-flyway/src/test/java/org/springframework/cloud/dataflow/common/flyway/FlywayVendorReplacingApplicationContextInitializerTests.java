/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.cloud.dataflow.common.flyway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link FlywayVendorReplacingApplicationContextInitializer}.
 */
class FlywayVendorReplacingApplicationContextInitializerTests {

	@ParameterizedTest(name = "{0}")
	@MethodSource("vendorReplacedProperlyProvider")
	void vendorReplacedProperly(boolean usingMySqlUrl, boolean usingMariaDriver, List<String> configuredLocationProps, List<String> finalLocationProps) {
		List<String> props = new ArrayList<>();
		props.add("spring.datasource.url=" + (usingMySqlUrl ? "jdbc:mysql://localhost:3306/dataflow?permitMysqlScheme" : "jdbc:mariadb://localhost:3306/dataflow"));
		props.add("spring.datasource.driver-class-name=" + (usingMariaDriver ? "org.mariadb.jdbc.Driver" : "org.mysql.jdbc.Driver"));
		props.addAll(configuredLocationProps);

		// Prime an actual env by running it through the AppContextRunner with the configured properties
		new ApplicationContextRunner().withPropertyValues(props.toArray(new String[0])).run((context) -> {
			ConfigurableEnvironment env = context.getEnvironment();

			// Sanity check the locations props are as expected
			configuredLocationProps.forEach((location) -> {
				String key = location.split("=")[0];
				String value = location.split("=")[1];
				assertThat(env.getProperty(key)).isEqualTo(value);
			});

			// Run the env through the ACI
			FlywayVendorReplacingApplicationContextInitializer flywayVendorReplacingInitializer = new FlywayVendorReplacingApplicationContextInitializer();
			flywayVendorReplacingInitializer.initialize(context);

			// Verify they are replaced as expected
			finalLocationProps.forEach((location) -> {
				String key = location.split("=")[0];
				String value = location.split("=")[1];
				assertThat(env.getProperty(key)).isEqualTo(value);
			});
		});
	}

	private static Stream<Arguments> vendorReplacedProperlyProvider() {
		return Stream.of(
				arguments(Named.of("singleLocationWithVendor",true), true,
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/{vendor}"),
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/mysql")
				),
				arguments(Named.of("singleLocationWithoutVendor",true), true,
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/foo"),
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/foo")
				),
				arguments(Named.of("noLocations",true), true,
						Collections.emptyList(),
						Collections.emptyList()
				),
				arguments(Named.of("multiLocationsAllWithVendor",true), true,
						Arrays.asList(
								"spring.flyway.locations[0]=classpath:org/skipper/db0/{vendor}",
								"spring.flyway.locations[1]=classpath:org/skipper/db1/{vendor}",
								"spring.flyway.locations[2]=classpath:org/skipper/db2/{vendor}"),
						Arrays.asList(
								"spring.flyway.locations[0]=classpath:org/skipper/db0/mysql",
								"spring.flyway.locations[1]=classpath:org/skipper/db1/mysql",
								"spring.flyway.locations[2]=classpath:org/skipper/db2/mysql")
				),
				arguments(Named.of("multiLocationsSomeWithVendor",true), true,
						Arrays.asList(
								"spring.flyway.locations[0]=classpath:org/skipper/db0/{vendor}",
								"spring.flyway.locations[1]=classpath:org/skipper/db1/foo",
								"spring.flyway.locations[2]=classpath:org/skipper/db2/{vendor}"),
						Arrays.asList(
								"spring.flyway.locations[0]=classpath:org/skipper/db0/mysql",
								"spring.flyway.locations[1]=classpath:org/skipper/db1/foo",
								"spring.flyway.locations[2]=classpath:org/skipper/db2/mysql")
				),
				arguments(Named.of("multiLocationsNoneWithVendor",true), true,
						Arrays.asList(
								"spring.flyway.locations[0]=classpath:org/skipper/db0/foo",
								"spring.flyway.locations[1]=classpath:org/skipper/db1/bar",
								"spring.flyway.locations[2]=classpath:org/skipper/db2/zaa"),
						Arrays.asList(
								"spring.flyway.locations[0]=classpath:org/skipper/db0/foo",
								"spring.flyway.locations[1]=classpath:org/skipper/db1/bar",
								"spring.flyway.locations[2]=classpath:org/skipper/db2/zaa")
				),
				arguments(Named.of("mariaUrlWithMariaDriverDoesNotReplace",false), true,
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/{vendor}"),
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/{vendor}")
				),
				arguments(Named.of("mysqlUrlWithMysqlDriverDoesNotReplace",true), false,
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/{vendor}"),
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/{vendor}")
				),
				arguments(Named.of("mariaUrlMysqlDriverDoesNotReplace",false), false,
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/{vendor}"),
						Collections.singletonList("spring.flyway.locations[0]=classpath:org/skipper/db/{vendor}")
				)
		);
	}
}
