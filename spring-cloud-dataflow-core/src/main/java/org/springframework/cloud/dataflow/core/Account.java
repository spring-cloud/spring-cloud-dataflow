/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.core;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Accounts Entity
 * 
 * @author Carlos Miquel
 */
@Entity
@Table(name = "ACCOUNTS")
public class Account {

	/**
	 * Name of account.
	 */
	@Id
	@Column(name = "ACCOUNT_NAME")
	private String accountName;

	@Column(name = "DEPLOYMENT_PROPERTIES")
	private String deploymentProperties;

	/**
	 * Empty constructor
	 */
	public Account() {

	}

	/**
	 * All Args Constructor
	 * @param accountName Account Identifier
	 * @param deploymentProperties Deployment Properties
	 */
	public Account(String accountName, String deploymentProperties) {
		super();
		this.accountName = accountName;
		this.deploymentProperties = deploymentProperties;
	}

	/**
	 * @return the accountName
	 */
	public String getAccountName() {
		return accountName;
	}

	/**
	 * @param accountName the accountName to set
	 */
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	/**
	 * @return the deploymentProperties
	 */
	public String getDeploymentProperties() {
		return deploymentProperties;
	}

	/**
	 * @param deploymentProperties the deploymentProperties to set
	 */
	public void setDeploymentProperties(String deploymentProperties) {
		this.deploymentProperties = deploymentProperties;
	}

}
