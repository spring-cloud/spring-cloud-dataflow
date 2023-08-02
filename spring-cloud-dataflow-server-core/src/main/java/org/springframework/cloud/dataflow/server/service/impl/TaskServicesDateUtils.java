/*
 * Copyright 2016-2023 the original author or authors.
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

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Used to provide basic date functionality for the task services.
 *
 * @author Tobias Soloschenko
 */
public class TaskServicesDateUtils {

	/**
	 * Gets the date for the given days in the past.
	 *
	 * @param days the days to calculate the date with
	 * @return the date in the past
	 */
	public static Date getDateBeforeDays(@NotNull Integer days) {
		LocalDateTime localDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT).minusDays(days);
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}
}
