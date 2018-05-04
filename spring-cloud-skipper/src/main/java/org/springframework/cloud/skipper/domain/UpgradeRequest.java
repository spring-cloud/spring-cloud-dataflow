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
package org.springframework.cloud.skipper.domain;

/**
 * This contains all the request attributes for upgrade operation.
 *
 * @author Ilayaperumal Gopinathan
 */
public class UpgradeRequest {

	private PackageIdentifier packageIdentifier;
	private UpgradeProperties upgradeProperties;
	private Long timeout;

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

	@Override
	public String toString() {
		return "UpgradeRequest [packageIdentifier=" + packageIdentifier + ", upgradeProperties=" + upgradeProperties
				+ ", timeout=" + timeout + "]";
	}
}
