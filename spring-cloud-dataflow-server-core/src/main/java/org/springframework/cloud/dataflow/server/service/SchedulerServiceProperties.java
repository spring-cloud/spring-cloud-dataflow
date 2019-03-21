/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;

/**
 * Specifies the properties required to configure how data flow handles schedules.
 *
 * @author Glenn Renfro
 */
@ConfigurationProperties(prefix = SchedulerServiceProperties.SCHEDULER_SERVICE_PREFIX)
public class SchedulerServiceProperties {
	public static final String SCHEDULER_SERVICE_PREFIX = DataFlowPropertyKeys.PREFIX + "scheduler.service";

	public static final int SCHEDULER_MAX_RETURNED_NUMBER = 10000;

	/**
	 * Establish the maximum number of schedules to return in a single request.
	 */
	private int maxSchedulesReturned = SCHEDULER_MAX_RETURNED_NUMBER;

	public int getMaxSchedulesReturned() {
		return maxSchedulesReturned;
	}

	public void setMaxSchedulesReturned(int maxSchedulesReturned) {
		this.maxSchedulesReturned = maxSchedulesReturned;
	}
}
