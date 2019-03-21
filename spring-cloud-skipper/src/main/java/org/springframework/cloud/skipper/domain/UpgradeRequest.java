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
package org.springframework.cloud.skipper.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * This contains all the request attributes for upgrade operation.
 *
 * @author Ilayaperumal Gopinathan
 */
public class UpgradeRequest {

	private PackageIdentifier packageIdentifier;

	private UpgradeProperties upgradeProperties;

	private Long timeout;

	private boolean force;

	private List<String> appNames = new ArrayList<>();

	public PackageIdentifier getPackageIdentifier() {
		return packageIdentifier;
	}

	public void setPackageIdentifier(PackageIdentifier packageIdentifier) {
		this.packageIdentifier = packageIdentifier;
	}

	public UpgradeProperties getUpgradeProperties() {
		return upgradeProperties;
	}

	public void setUpgradeProperties(UpgradeProperties upgradeProperties) {
		this.upgradeProperties = upgradeProperties;
	}

	public Long getTimeout() {
		return timeout;
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public List<String> getAppNames() {
		return appNames;
	}

	public void setAppNames(List<String> appNames) {
		this.appNames = appNames;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("UpgradeRequest{");
		sb.append("packageIdentifier=").append(packageIdentifier);
		sb.append(", upgradeProperties=").append(upgradeProperties);
		sb.append(", timeout=").append(timeout);
		sb.append(", force=").append(force);
		sb.append(", appNames=").append(appNames);
		sb.append('}');
		return sb.toString();
	}
}
