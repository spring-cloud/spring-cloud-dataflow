/*
 * Copyright 2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Turanski
 **/
public abstract class AbstractTaskPlatformFactory<P extends AbstractPlatformProperties<?>> implements TaskPlatformFactory {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	protected final P platformProperties;

	private final String platformType;

	protected AbstractTaskPlatformFactory(P platformProperties, String platformType){
		this.platformProperties = platformProperties;
		this.platformType = platformType;
	}

	@Override
	public TaskPlatform createTaskPlatform() {
		return new TaskPlatform(platformType, createLaunchers());
	}

	protected List<Launcher> createLaunchers() {
		List<Launcher> launchers = new ArrayList<>();

		for (String account : platformProperties.getAccounts().keySet()) {
			try {
				launchers.add(createLauncher(account));
			}
			catch (Exception e) {
				logger.error("{} platform account [{}] could not be registered: {}",
					platformType, account, e);
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		return launchers;
	}
}
