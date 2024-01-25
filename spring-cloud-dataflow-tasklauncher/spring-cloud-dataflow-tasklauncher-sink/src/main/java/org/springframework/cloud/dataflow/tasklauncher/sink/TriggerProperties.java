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

package org.springframework.cloud.dataflow.tasklauncher.sink;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author David Turanski
 **/
@ConfigurationProperties(prefix = "trigger")
@Validated
public class TriggerProperties {
	/**
	 * The initial delay in milliseconds.
	 */
	private int initialDelay = 1000;

	/**
	 * The polling period in milliseconds.
	 */
	private int period = 1000;

	/**
	 * The maximum polling period in milliseconds. Will be set to period if period >
	 * maxPeriod.
	 */
	private int maxPeriod = 30000;

	@Min(0)
	public int getInitialDelay() {
		return initialDelay;
	}

	public void setInitialDelay(int initialDelay) {
		this.initialDelay = initialDelay;
	}

	@Min(0)
	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	@Min(1000)
	public int getMaxPeriod() {
		return maxPeriod;
	}

	public void setMaxPeriod(int maxPeriod) {
		this.maxPeriod = maxPeriod;
	}

	@PostConstruct
	public void checkMaxPeriod() {
		maxPeriod = Integer.max(maxPeriod, period);
	}
}
