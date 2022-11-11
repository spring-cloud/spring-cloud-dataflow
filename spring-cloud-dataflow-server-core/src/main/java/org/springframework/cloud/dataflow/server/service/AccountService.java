/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service;

import java.util.List;

import javassist.NotFoundException;

import org.springframework.cloud.dataflow.rest.resource.AccountResource;

/**
 * Provides account operations.
 *
 */
public interface AccountService {

	/**
	 * List all accounts.
	 * @return List of accounts from table.
	 */
	public List<AccountResource> list();

	/**
	 * Save new Account in database
	 * @param createAccountRequest Create new account request
	 * @return Created Account
	 */
	public AccountResource save(AccountResource createAccountRequest);

	/**
	 * Delete requested account
	 * @param accountName Account Identifier
	 */
	public void delete(String accountName);

	/**
	 * Detail requested account
	 * @param accountName Account Identifier
	 * @return Account detail
	 * @throws NotFoundException 
	 */
	public AccountResource detail(String accountName) throws NotFoundException;
}
