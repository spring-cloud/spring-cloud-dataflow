/*
 * Copyright 2015-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javassist.NotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.Account;
import org.springframework.cloud.dataflow.rest.resource.AccountResource;
import org.springframework.cloud.dataflow.server.mapper.AccountMapper;
import org.springframework.cloud.dataflow.server.repository.AccountRepository;
import org.springframework.cloud.dataflow.server.service.AccountService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of the {@link AccountService} interface. Provide service methods
 * for Accounts.
 */
@Transactional
public class DefaultAccountService implements AccountService {

	/**
	 * Logger
	 */
	private static final Log log = LogFactory.getLog(DefaultAccountService.class);

	private AccountMapper accountMapper;

	/**
	 * Account Repository
	 */
	private AccountRepository accountRepository;

	public DefaultAccountService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
		this.accountMapper = new AccountMapper();
	}

	@Override
	public List<AccountResource> list() {
		List<AccountResource> response = new ArrayList<>();
		log.info("Entering accounts list service.");
		Iterable<Account> res = accountRepository.findAll();
		for (Account account : res) {
			response.add(this.accountMapper.map(account));
		}
		return response;
	}

	@Override
	public AccountResource save(AccountResource createAccountRequest) {
		Account accountEntity = this.accountMapper.map(createAccountRequest);
		Account res = this.accountRepository.save(accountEntity);
		return this.accountMapper.map(res);
	}

	@Override
	public void delete(String namespace) {
		this.accountRepository.deleteById(namespace);
	}

	@Override
	public AccountResource detail(String namespace) throws NotFoundException {
		Optional<Account> res = this.accountRepository.findById(namespace);
		if (res.isPresent()) {
			return this.accountMapper.map(res.get());
		}
		throw new NotFoundException("Account not found in database");
	}

}
