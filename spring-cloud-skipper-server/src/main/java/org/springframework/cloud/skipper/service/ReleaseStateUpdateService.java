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

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Service which schedules background updates for applications known to {@link ReleaseRepository}.
 *
 * @author Janne Valkealahti
 *
 */
@Service
public class ReleaseStateUpdateService {

	private static final Logger log = LoggerFactory.getLogger(ReleaseStateUpdateService.class);
	private final ReleaseService releaseService;
	private final ReleaseRepository releaseRepository;
	private long nextFullPoll;

	/**
	 * Instantiates a new release state update service.
	 *
	 * @param releaseService the release service
	 * @param releaseRepository the release repository
	 */
	public ReleaseStateUpdateService(ReleaseService releaseService, ReleaseRepository releaseRepository) {
		Assert.notNull(releaseService, "'releaseService' must be set");
		Assert.notNull(releaseRepository, "'releaseRepository' must be set");
		this.releaseService = releaseService;
		this.releaseRepository = releaseRepository;
		this.nextFullPoll = getNextFullPoll();
		log.info("Setting up ReleaseStateUpdateService");
	}

	@Scheduled(initialDelay = 5000, fixedRate = 5000)
	public void update() {
		long now = System.currentTimeMillis();
		boolean fullPoll = now > nextFullPoll;
		if (fullPoll) {
			// setup next full poll
			nextFullPoll = getNextFullPoll();
			log.debug("Setup next full poll at {}", new Date(nextFullPoll));
		}
		Iterable<Release> releases = releaseRepository.findLatestDeployedOrFailed();
		for (Release release : releases) {
			Info info = release.getInfo();
			if (info != null) {
				// poll new apps every time or we do full poll anyway
				boolean poll = fullPoll || (info.getLastDeployed().getTime() > (now - 120000));
				if (poll) {
					try {
						release = releaseService.status(release);
						log.debug("New Release state {} {}", release.getName(), release.getInfo().getStatus(),
								release.getInfo().getStatus() != null
										? release.getInfo().getStatus().getPlatformStatus() : "");
						releaseRepository.save(release);
					}
					catch (Exception e) {
						log.warn("Unable to update release status", e);
					}
				}
			}
		}
	}

	/**
	 * Return next poll time in future in two minute intervals.
	 *
	 * @return estimate of next full poll time
	 */
	private long getNextFullPoll() {
		long next = System.currentTimeMillis() + 120000;
		return next - (next % 120000);
	}
}
