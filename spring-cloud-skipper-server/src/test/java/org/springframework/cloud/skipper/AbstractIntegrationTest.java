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
package org.springframework.cloud.skipper;

import org.junit.After;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.cloud.skipper.service.ReleaseService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class to implement transactional integration tests using the root application
 * configuration.
 *
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public abstract class AbstractIntegrationTest {

	private final Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);

	@Autowired
	protected ReleaseRepository releaseRepository;

	@Autowired
	protected ReleaseService releaseService;

	@After
	public void cleanupReleases() {
		// Add a sleep for now to give the local deployer a chance to install the app. This
		// should go away once we introduce spring state machine.
		try {
			Thread.sleep(5000);
			for (Release release : releaseRepository.findAll()) {
				if (release.getInfo().getStatus().getStatusCode() != StatusCode.DELETED) {
					releaseService.delete(release.getName());
				}
			}
		}
		catch (Exception e) {
			logger.error("error cleaning up resource in integration test");
		}
	}
}
