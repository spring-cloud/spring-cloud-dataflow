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
package org.springframework.cloud.dataflow.server.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.dataflow.core.Account;
import org.springframework.cloud.dataflow.rest.resource.AccountResource;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;

/**
 * Account Objects Mapper
 * 
 * @author Carlos Miquel Garrido
 */
public class AccountMapper {

	private ObjectMapper om = new ObjectMapper();

	public AccountResource map(Account account) {
		KubernetesDeployerProperties properties;
		try {
			properties = om.readValue(account.getDeploymentProperties(), KubernetesDeployerProperties.class);
		}
		catch (JsonProcessingException e) {
			properties = new KubernetesDeployerProperties();
		}
		AccountResource accountResource = new AccountResource();
		accountResource.setAccountName(account.getAccountName());
		accountResource.setProperties(properties);
		return accountResource;
	}

	public Account map(AccountResource accountResource) {

		String deploymentProperties;
		try {
			deploymentProperties = om.writeValueAsString(accountResource.getProperties());
		}
		catch (JsonProcessingException e) {
			deploymentProperties = "{}";
		}

		Account account = new Account();
		account.setAccountName(accountResource.getAccountName());
		account.setDeploymentProperties(deploymentProperties);
		return account;
	}

}
