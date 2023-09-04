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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.lang.NonNull;

/**
 * Provides date functionality for the task services.
 *
 * @author Tobias Soloschenko
 */
final class TaskServicesDateUtils {

	private TaskServicesDateUtils() {
	}

	/**
	 * Gets the date representation for the given number of days in the past.
	 *
	 * @param numDaysAgo the number of days ago
	 * @return the date for {@code numDaysAgo} from today at midnight (locally)
	 */
	public static Date numDaysAgoFromLocalMidnightToday(@NonNull Integer numDaysAgo) {
		LocalDateTime localDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT).minusDays(numDaysAgo);
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}
}
