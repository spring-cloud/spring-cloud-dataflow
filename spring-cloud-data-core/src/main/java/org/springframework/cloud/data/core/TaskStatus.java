/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.core;

import java.util.Date;

/**
 * @author Glenn Renfro
 */
public class TaskStatus {
	
	private long jobExecutionId;
	private long version;
	private long jobInstanceid;
	private Date createTime;
	private Date startTime;
	private Date endTime;
	private String status;
	private String exitCode;
	private String exitMessage;
	private Date lastUpdated;
	private String jobConfigurationLocation;
	private String guid;

	public long getJobExecutionId() {
		return jobExecutionId;
	}

	public void setJobExecutionId(long jobExecutionId) {
		this.jobExecutionId = jobExecutionId;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public long getJobInstanceid() {
		return jobInstanceid;
	}

	public void setJobInstanceid(long jobInstanceid) {
		this.jobInstanceid = jobInstanceid;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getExitCode() {
		return exitCode;
	}

	public void setExitCode(String exitCode) {
		this.exitCode = exitCode;
	}

	public String getExitMessage() {
		return exitMessage;
	}

	public void setExitMessage(String exitMessage) {
		this.exitMessage = exitMessage;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public String getJobConfigurationLocation() {
		return jobConfigurationLocation;
	}

	public void setJobConfigurationLocation(String jobConfigurationLocation) {
		this.jobConfigurationLocation = jobConfigurationLocation;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}
}
