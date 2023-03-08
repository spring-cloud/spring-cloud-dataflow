/*
 * Copyright 2016-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.features;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.AccountRepository;
import org.springframework.cloud.dataflow.server.repository.DatabaseLauncherRepository;
import org.springframework.cloud.dataflow.server.service.AccountService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Account feature configuration
 * @author Carlos Miquel
 */
@Configuration
public class AccountConfiguration {

	/**
	 * Logger
	 */
	private static final Log log = LogFactory.getLog(AccountConfiguration.class);

	/**
	 * Account service bean
	 * @param accountRepository Account database repository.
	 * @return Account service.
	 */
	@Bean
	public AccountService accountService(AccountRepository accountRepository) {
		return new DefaultAccountService(accountRepository);
	}

	/**
	 * If account database feature is enabled, override launcher repository.
	 * @param accountRepository Account repository.
	 * @return Launcher repository.
	 */
	@Bean
	@ConditionalOnProperty(value = "spring.cloud.dataflow.task.platform.kubernetes.accounts.database", havingValue = "true", matchIfMissing = false)
	@Primary
	public LauncherRepository databaseLauncherRepository(AccountRepository accountRepository) {
		log.debug("Account info in database feature.");
		return new DatabaseLauncherRepository(accountRepository, false);
	}

}
