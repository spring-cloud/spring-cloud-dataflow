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
package org.springframework.cloud.skipper.server.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.deployer.ReleaseManagerFactory;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Service which schedules background updates for applications known to
 * {@link ReleaseRepository}.
 *
 * @author Janne Valkealahti
 * @author Glenn Renfro
 *
 */
public class ReleaseStateUpdateService {

	private static final Logger log = LoggerFactory.getLogger(ReleaseStateUpdateService.class);

	private final ReleaseManagerFactory releaseManagerFactory;

	private final ReleaseRepository releaseRepository;

	private long nextFullPoll;

	/**
	 * Instantiates a new release state update service.
	 *
	 * @param releaseManagerFactory the release manager factory
	 * @param releaseRepository the release repository
	 */
	public ReleaseStateUpdateService(ReleaseManagerFactory releaseManagerFactory,
			ReleaseRepository releaseRepository) {
		Assert.notNull(releaseManagerFactory, "'releaseManagerFactory' must be set");
		Assert.notNull(releaseRepository, "'releaseRepository' must be set");
		this.releaseManagerFactory = releaseManagerFactory;
		this.releaseRepository = releaseRepository;
		this.nextFullPoll = getNextFullPoll();
		log.info("Setting up ReleaseStateUpdateService");
	}

	@Scheduled(initialDelay = 5000, fixedRate = 5000)
	@Transactional
	public synchronized void update() {
		log.debug("Scheduled update state method running...");
		long now = System.currentTimeMillis();
		boolean fullPoll = now > this.nextFullPoll;
		if (fullPoll) {
			// setup next full poll
			this.nextFullPoll = getNextFullPoll();
			log.debug("Setup next full poll at {}", new Date(this.nextFullPoll));
		}
		Iterable<Release> releases = this.releaseRepository.findLatestDeployedOrFailed();
		for (Release release : releases) {
			String kind = ManifestUtils.resolveKind(release.getManifest().getData());
			ReleaseManager releaseManager = this.releaseManagerFactory.getReleaseManager(kind);

			Info info = release.getInfo();
			if (checkInfo(info)) {
				// poll new apps every time or we do full poll anyway
				boolean isNewApp = (info.getLastDeployed().getTime() > (now - 120000));
				log.debug("Considering updating state for {}-v{}", release.getName(), release.getVersion());
				log.debug("fullPoll = {}, isNewApp = {}", fullPoll, isNewApp);
				boolean poll = fullPoll || (isNewApp);
				if (poll) {
					try {
						release = releaseManager.status(release);
						log.debug("New Release state {} {}", release.getName(), release.getInfo().getStatus(),
								release.getInfo().getStatus() != null
										? release.getInfo().getStatus().getPlatformStatusPrettyPrint()
										: "");
						this.releaseRepository.save(release);
					}
					catch (Exception e) {
						log.warn("Unable to update release status for release " + release.getName() + "-v"
								+ release.getVersion(), e);
					}
				}
				else {
					log.debug("Not updating state for {}-v{}", release.getName(), release.getVersion());
				}
			}
		}
	}

	private boolean checkInfo(Info info) {
		if (info == null) {
			throw new IllegalStateException("Info can not be null.");
		}
		if (info.getLastDeployed() == null) {
			throw new IllegalStateException("Info.LastDeployed can not be null.");
		}
		return true;
	}

	/**
	 * Return next poll time in future in 5 minute intervals.
	 *
	 * @return estimate of next full poll time
	 */
	private long getNextFullPoll() {
		long next = System.currentTimeMillis() + 600000;
		return next - (next % 600000);
	}
}
