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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.List;

import javax.sql.DataSource;

import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.Account;
import org.springframework.cloud.dataflow.rest.resource.AccountResource;
import org.springframework.cloud.dataflow.server.configuration.AccountServiceDependencies;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.repository.AccountRepository;
import org.springframework.cloud.dataflow.server.service.AccountService;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Carlos Miquel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { TaskServiceDependencies.class, AccountServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class DefaultAccountServiceTests {

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	DataSource dataSource;

	@Autowired
	AccountService accountService;

	@BeforeEach
	public void setup() {
	}

	@Test
	void findAllTest() {
		setupFindAllTest();
		List<AccountResource> testResponse = this.accountService.list();
		assertEquals(30, testResponse.size());
		cleanRepository();
	}

	private void setupFindAllTest() {
		for (int i = 0; i < 30; i++) {
			this.accountRepository.save(new Account("AccountName" + i, ""));
		}
	}

	@Test
	void saveTest() {
		AccountResource testResponse = this.accountService
				.save(new AccountResource("accountName", new KubernetesDeployerProperties()));
		assertEquals("accountName", testResponse.getAccountName());
		assertEquals(1, accountRepository.count());
		cleanRepository();
	}

	@Test
	void detailTest() throws NotFoundException {
		this.accountRepository.save(new Account("AccountName", ""));

		AccountResource testResponse = this.accountService.detail("AccountName");
		assertEquals("AccountName", testResponse.getAccountName());
		cleanRepository();
	}

	@Test()
	void detailNotFoundTest() {
		assertThrows(NotFoundException.class, () -> this.accountService.detail("AccountName"), "Exception not thrown");
		cleanRepository();
	}

	@Test
	void deleteTest() {
		this.accountRepository.save(new Account("AccountName", ""));

		this.accountService.delete("AccountName");
		assertEquals(0, this.accountRepository.count());
		assertFalse(this.accountRepository.findById("AccountName").isPresent());
		cleanRepository();
	}

	@Test
	void deleteNotFoundTest() {
		assertThrows(EmptyResultDataAccessException.class, () -> this.accountService.delete("AccountName"),
				"Exception not thrown");
		cleanRepository();
	}

	private void cleanRepository() {
		this.accountRepository.deleteAll();
	}

}
