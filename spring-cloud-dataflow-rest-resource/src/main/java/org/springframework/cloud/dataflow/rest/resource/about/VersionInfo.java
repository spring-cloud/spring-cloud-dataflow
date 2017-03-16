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

package org.springframework.cloud.dataflow.rest.resource.about;

import java.util.Date;

import org.springframework.cloud.dataflow.rest.Version;

/**
 * Provides version information about core libraries used.
 *
 * @author Gunnar Hillert
 */
public class VersionInfo {

	/**
	 * Default constructor for serialization frameworks.
	 */
	public VersionInfo() {
	}

	private Dependency implementation;
	private Dependency core;
	private Dependency dashboard;
	private Dependency shell;

	private String commitId;
	private String shortCommitId;
	private Date   commitTime;
	private String branch;

	private int restApiRevision = Version.REVISION;

	public Integer getRestApiRevision() {
		return this.restApiRevision;
	}

	public Dependency getCore() {
		return core;
	}

	public Dependency getImplementation() {
		return implementation;
	}

	public void setImplementation(Dependency implementation) {
		this.implementation = implementation;
	}

	public void setCore(Dependency core) {
		this.core = core;
	}
	public Dependency getDashboard() {
		return dashboard;
	}
	public void setDashboard(Dependency dashboard) {
		this.dashboard = dashboard;
	}
	public Dependency getShell() {
		return shell;
	}
	public void setShell(Dependency shell) {
		this.shell = shell;
	}

	public String getCommitId() {
		return commitId;
	}

	public void setCommitId(String commitId) {
		this.commitId = commitId;
	}

	public String getShortCommitId() {
		return shortCommitId;
	}

	public void setShortCommitId(String shortCommitId) {
		this.shortCommitId = shortCommitId;
	}

	public Date getCommitTime() {
		return commitTime;
	}

	public void setCommitTime(Date commitTime) {
		this.commitTime = commitTime;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

}
