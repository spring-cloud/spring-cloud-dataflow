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

import java.util.Date;

/**
 * @author Mark Pollack
 */
public class Info {

	private Status status;

	private Date firstDeployed;

	private Date lastDeployed;

	// Deleted tracks when this object was deleted.
	private Date deleted;

	// Description is human-friendly "log entry" about this release.
	private String description;

	public Info() {
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Date getFirstDeployed() {
		return firstDeployed;
	}

	public void setFirstDeployed(Date firstDeployed) {
		this.firstDeployed = firstDeployed;
	}

	public Date getLastDeployed() {
		return lastDeployed;
	}

	public void setLastDeployed(Date lastDeployed) {
		this.lastDeployed = lastDeployed;
	}

	public Date getDeleted() {
		return deleted;
	}

	public void setDeleted(Date deleted) {
		this.deleted = deleted;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
