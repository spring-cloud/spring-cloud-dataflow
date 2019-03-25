/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author David Turanski
 **/
public abstract class AbstractPlatformProperties<P> {
	private Map<String, P> accounts = new LinkedHashMap<>();

	public Map<String, P> getAccounts() {
		return accounts;
	}

	public void setAccounts(Map<String, P> accounts) {
		this.accounts = accounts;
	}

	public P accountProperties(String account) {
		P properties = this.getAccounts().get(account);
		if (properties == null) {
			throw new IllegalArgumentException("Account " + account + " does not exist");
		}
		return properties;
	}

}
