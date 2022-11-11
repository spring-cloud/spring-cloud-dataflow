/*
 * Copyright 2021 the original author or authors.
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.Account;
import org.springframework.cloud.dataflow.rest.resource.AccountResource;
import org.springframework.cloud.dataflow.server.configuration.AccountServiceDependencies;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Carlos Miquel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { TaskServiceDependencies.class, AccountServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class AccountMapperTest {

	private AccountMapper accountMapper = new AccountMapper();

	private ObjectMapper om = new ObjectMapper();

	@Test
	void mapAccountTest() throws JsonProcessingException {
		AccountResource accountResource = generateAccountResource();
		Account testResponse = this.accountMapper.map(accountResource);
		assertEquals("accountName", testResponse.getAccountName());
		assertEquals(om.writeValueAsString(accountResource.getProperties()), testResponse.getDeploymentProperties());
	}

	private AccountResource generateAccountResource() {
		AccountResource accountResource = new AccountResource();
		accountResource.setAccountName("accountName");
		KubernetesDeployerProperties kubernetesDeployerProperties = new KubernetesDeployerProperties();
		accountResource.setProperties(kubernetesDeployerProperties);
		return accountResource;
	}

	@Test
	void mapAccountResourceTest() {
		Account account = generateAccount();
		AccountResource testResponse = this.accountMapper.map(account);
		assertEquals("accountName", testResponse.getAccountName());
		assertEquals("namespaceName", testResponse.getProperties().getNamespace());
	}

	public static Account generateAccount() {
		Account account = new Account();
		account.setAccountName("accountName");
		account.setDeploymentProperties("{\"namespace\":\"namespaceName\"}");
		return account;
	}

}
