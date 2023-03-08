/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.List;

import javassist.NotFoundException;

import org.springframework.cloud.dataflow.core.Account;
import org.springframework.cloud.dataflow.rest.resource.AccountResource;
import org.springframework.cloud.dataflow.server.service.AccountService;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link Account}. This includes CRUD operations.
 * 
 * @author Carlos Miquel
 */
@RestController
@RequestMapping("/accounts")
@ExposesResourceFor(AccountResource.class)
public class AccountController {

	/**
	 * Service with Account operations
	 */
	private AccountService accountService;

	/**
	 * Creates a {@code AccountController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link AccountService}</li>
	 * </ul>
	 * @param accountService Account service with account operations.
	 */
	public AccountController(AccountService accountService) {
		Assert.notNull(accountService, "accountService must not be null");
		this.accountService = accountService;
	}

	/**
	 * Return a list of {@link AccountResource} defined tasks.
	 *
	 */
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public List<AccountResource> list() {
		return this.accountService.list();
	}

	/**
	 * Return the detail of the requested {@link AccountResource}.
	 * @throws NotFoundException In case no account was found
	 *
	 */
	@GetMapping("/{namespace}")
	@ResponseStatus(HttpStatus.OK)
	public AccountResource detail(@PathVariable String namespace) throws NotFoundException {
		return this.accountService.detail(namespace);
	}

	/**
	 * Saves the input {@link AccountResource} into the database.
	 *
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResource save(@RequestBody AccountResource createAccountRequest) {
		return this.accountService.save(createAccountRequest);
	}

	/**
	 * Deletes the requested {@link AccountResource}.
	 *
	 */
	@DeleteMapping("/{namespace}")
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable String namespace) {
		this.accountService.delete(namespace);
	}

}
