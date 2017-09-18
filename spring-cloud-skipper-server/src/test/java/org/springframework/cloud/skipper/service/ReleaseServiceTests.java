/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.service;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractIntegrationTest;
import org.springframework.cloud.skipper.domain.skipperpackage.DeployProperties;
import org.springframework.cloud.skipper.index.PackageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Mark Pollack
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "spring.cloud.skipper.server.synchonizeIndexOnContextRefresh=true" })
public class ReleaseServiceTests extends AbstractIntegrationTest {

	@Autowired
	private ReleaseService releaseService;

	@Test
	public void testBadArguments() {
		assertThatThrownBy(() -> releaseService.deploy("badId", new DeployProperties()))
				.isInstanceOf(PackageException.class);

		assertThatThrownBy(() -> releaseService.rollback("badId", -1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("less than zero");

		assertThatThrownBy(() -> releaseService.rollback("badId", 1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Could not find release = [badId]");


	}
}
