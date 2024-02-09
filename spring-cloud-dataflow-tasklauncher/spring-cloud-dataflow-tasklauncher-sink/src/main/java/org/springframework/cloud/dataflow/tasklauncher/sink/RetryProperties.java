/*
 * Copyright 2024 the original author or authors.
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * @author Corneil du Plessis
 **/
@ConfigurationProperties(prefix = "retry")
@Validated
public class RetryProperties {
	/**
	 * The initial delay in milliseconds.
	 */
	private int initialDelay = 1000;

	/**
	 * The multiplier used by retry template exponential backoff.
	 */
	private double multiplier = 1.5;

	/**
	 * The maximum polling period in milliseconds. Must be greater than initialDelay.
	 */
	private int maxPeriod = 30000;

	/**
	 * Maximum number of attempts
	 */
	private int maxAttempts = -1;

	@Min(100)
	public int getInitialDelay() {
		return initialDelay;
	}

	public void setInitialDelay(int initialDelay) {
		this.initialDelay = initialDelay;
	}

	@DecimalMin("1.0")
	public double getMultiplier() {
		return multiplier;
	}

	public void setMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}

	@Min(1000)
	public int getMaxPeriod() {
		return maxPeriod;
	}

	public void setMaxPeriod(int maxPeriod) {
		this.maxPeriod = maxPeriod;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	@PostConstruct
	public void checkMaxPeriod() {
		Assert.isTrue(maxPeriod > initialDelay, "maxPeriod must be greater than initialDelay");
	}

	@Override
	public String toString() {
		return "RetryProperties{" +
			"initialDelay=" + initialDelay +
			", multiplier=" + multiplier +
			", maxPeriod=" + maxPeriod +
			", maxAttempts=" + maxAttempts +
			'}';
	}
}
