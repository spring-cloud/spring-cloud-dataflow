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
package org.springframework.cloud.dataflow.server.repository;

import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.Account;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.mapper.AccountLauncherMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom repository to replace basic LauncherRepository and access the database instead.
 * Only read actions implemented.
 *
 */
@Transactional
public class DatabaseLauncherRepository implements LauncherRepository {

	/**
	 * Logger
	 */
	private static final Log log = LogFactory.getLog(DatabaseLauncherRepository.class);

	/**
	 * Account database repository.
	 */
	private AccountRepository accountRepository;

	/**
	 * Account Mapper
	 */
	private AccountLauncherMapper accountLauncherMapper;

	/**
	 * Constructor
	 * @param accountRepository Account database repository
	 * @param schedulesEnabled Schedules enabled by configuration
	 */
	public DatabaseLauncherRepository(AccountRepository accountRepository, boolean schedulesEnabled) {
		this.accountRepository = accountRepository;
		this.accountLauncherMapper = new AccountLauncherMapper(schedulesEnabled);
	}

	@Override
	public Iterable<Launcher> findAll(Sort sort) {
		Iterable<Account> databaseResponse = this.accountRepository.findAll(sort);
		return this.accountLauncherMapper.map(databaseResponse);
	}

	@Override
	public Page<Launcher> findAll(Pageable pageable) {
		throw new NotImplementedException();
	}

	@Override
	public <S extends Launcher> S save(S entity) {
		// Not implemented
		return null;
	}

	@Override
	public <S extends Launcher> Iterable<S> saveAll(Iterable<S> entities) {
		throw new NotImplementedException();
	}

	@Override
	public Optional<Launcher> findById(String id) {
		Optional<Account> response = this.accountRepository.findById(id);
		if (response.isPresent()) {
			log.info("Account retrieved in findByName: " + response.get().getAccountName());
			return Optional.of(this.accountLauncherMapper.map(response.get()));
		}
		return Optional.empty();
	}

	@Override
	public boolean existsById(String id) {
		Optional<Account> response = this.accountRepository.findById(id);
		return response.isPresent();
	}

	@Override
	public Iterable<Launcher> findAll() {
		Iterable<Account> databaseResponse = this.accountRepository.findAll();
		return this.accountLauncherMapper.map(databaseResponse);
	}

	@Override
	public Iterable<Launcher> findAllById(Iterable<String> ids) {
		ArrayList<Launcher> response = new ArrayList<>();
		for (String id : ids) {
			Optional<Launcher> launcher = this.findById(id);
			if (launcher.isPresent()) {
				response.add(launcher.get());
			}
		}
		return response;
	}

	@Override
	public long count() {
		return this.accountRepository.count();
	}

	@Override
	public void deleteById(String id) {
		throw new NotImplementedException();

	}

	@Override
	public void delete(Launcher entity) {
		throw new NotImplementedException();
	}

	@Override
	public void deleteAllById(Iterable<? extends String> ids) {
		throw new NotImplementedException();
	}

	@Override
	public void deleteAll(Iterable<? extends Launcher> entities) {
		throw new NotImplementedException();
	}

	@Override
	public void deleteAll() {
		throw new NotImplementedException();
	}

	@Override
	public Launcher findByName(String name) {
		Optional<Account> response = this.accountRepository.findById(name);
		if (response.isPresent()) {
			log.info("Account retrieved in findByName: " + response.get().getAccountName());
			return this.accountLauncherMapper.map(response.get());
		}
		return null;
	}

}
