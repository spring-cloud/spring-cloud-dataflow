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
import org.springframework.cloud.skipper.repository.RepositoryRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
@ActiveProfiles("index-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class RepositoryInitializationServiceTest extends AbstractIntegrationTest {

	@Autowired
	private RepositoryRepository repositoryRepository;

	@Test
	public void intialize() throws Exception {
		// There will always be the local repository
		assertThat(repositoryRepository.count()).isEqualTo(2);
		assertThat(repositoryRepository.findByName("test").getUrl()).isEqualTo("classpath:");
	}

}
