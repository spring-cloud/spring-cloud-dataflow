/*
 * Copyright 2017-2018 the original author or authors.
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

import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Basic information about the package deployment operation.
 *
 * @author Mark Pollack
 * @author Gunnar Hillert
 */
@Entity
@Table(name = "SkipperInfo")
public class Info extends AbstractEntity {

	@OneToOne(cascade = { CascadeType.ALL })
	@JoinColumn(foreignKey = @ForeignKey(name = "fk_info_status"))
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

	/**
	 * Create a new Info instance with the given description, first deployed and last deployed dates
	 * set to the current date-time, and StatusCode.UNKNOWN.
	 * @param description a string describing the general info status at a level finer than StatusCode
	 * @return a new Info object
	 */
	public static Info createNewInfo(String description) {
		Info info = new Info();
		info.setFirstDeployed(new Date());
		info.setLastDeployed(new Date());
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		info.setStatus(status);
		info.setDescription(description);
		return info;
	}
}
