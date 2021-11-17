/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.repository.jpa;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class ReleaseRepositoryImpl implements ReleaseRepositoryCustom {

	@Autowired
	private ObjectProvider<ReleaseRepository> releaseRepository;

	@Override
	public Release findLatestRelease(String releaseName) {
		Release latestRelease = this.releaseRepository.getIfAvailable().findTopByNameOrderByVersionDesc(releaseName);
		if (latestRelease == null) {
			throw new ReleaseNotFoundException(releaseName);
		}
		return latestRelease;
	}

	@Override
	public Release findLatestDeployedRelease(String releaseName) {
		List<Release> releases = this.releaseRepository.getIfAvailable().findByNameOrderByVersionDesc(releaseName);
		for (Release release : releases) {
			if (release.getInfo().getStatus().getStatusCode().equals(StatusCode.DEPLOYED)) {
				return release;
			}
		}
		throw new ReleaseNotFoundException(releaseName);
	}

	@Override
	public Release findLatestReleaseForUpdate(String releaseName) {
		List<Release> releases = this.releaseRepository.getIfAvailable().findByNameOrderByVersionDesc(releaseName);
		for (Release release : releases) {
			if (release.getInfo().getStatus().getStatusCode().equals(StatusCode.DEPLOYED) ||
					release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
				return release;
			}
		}
		throw new ReleaseNotFoundException(releaseName);
	}

	@Override
	public Release findReleaseToRollback(String releaseName) {
		Release latestRelease = this.releaseRepository.getIfAvailable().findLatestReleaseForUpdate(releaseName);
		List<Release> releases = this.releaseRepository.getIfAvailable().findByNameOrderByVersionDesc(releaseName);
		for (Release release : releases) {
			if ((release.getInfo().getStatus().getStatusCode().equals(StatusCode.DEPLOYED) ||
					release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) &&
					release.getVersion() != latestRelease.getVersion()) {
				return release;
			}
		}
		throw new ReleaseNotFoundException(releaseName);
	}

	@Override
	public Release findByNameAndVersion(String releaseName, int version) {
		Iterable<Release> releases = this.releaseRepository.getIfAvailable().findAll();

		Release matchingRelease = null;
		for (Release release : releases) {
			if (release.getName().equals(releaseName) && release.getVersion() == version) {
				matchingRelease = release;
				break;
			}
		}
		if (matchingRelease == null) {
			throw new ReleaseNotFoundException(releaseName, version);
		}
		return matchingRelease;
	}

	@Override
	public List<Release> findReleaseRevisions(String releaseName, Integer revisions) {
		int latestVersion = findLatestRelease(releaseName).getVersion();
		int lowerVersion = latestVersion - Integer.valueOf(revisions);
		return this.releaseRepository.getIfAvailable().findByNameAndVersionBetweenOrderByNameAscVersionDesc(releaseName,
				lowerVersion + 1, latestVersion);
	}

	@Override
	public List<Release> findLatestDeployedOrFailed(String releaseName) {
		return getDeployedOrFailed(this.releaseRepository.getIfAvailable().findByNameIgnoreCaseContaining(releaseName));
	}

	@Override
	public List<Release> findLatestDeployedOrFailed() {
		return getDeployedOrFailed(this.releaseRepository.getIfAvailable().findAll());
	}

	private List<Release> getDeployedOrFailed(Iterable<Release> allReleases) {
		List<Release> releases = new ArrayList<>();
		for (Release release : allReleases) {
			StatusCode releaseStatusCode = release.getInfo().getStatus().getStatusCode();
			if (releaseStatusCode.equals(StatusCode.DEPLOYED) || releaseStatusCode.equals(StatusCode.FAILED)) {
				releases.add(release);
			}
		}
		return releases;
	}

	@Override
	public Release findLatestReleaseIfDeleted(String releaseName) {
		Release latestRelease = this.releaseRepository.getIfAvailable().findTopByNameOrderByVersionDesc(releaseName);
		return (latestRelease != null &&
				latestRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) ? latestRelease : null;
	}

}
