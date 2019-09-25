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

package org.springframework.cloud.dataflow.scheduler.launcher;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.dataflow.scheduler.launcher.configuration.SchedulerTaskLauncherException;

import static org.hamcrest.CoreMatchers.is;

public class SchedulerTaskLauncherExceptionTests {
	public static final String ERROR_MESSAGE = "ERROR_MESSAGE";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMessage() {
		thrown.expect(SchedulerTaskLauncherException.class);
		thrown.expectMessage(ERROR_MESSAGE);
		throw new SchedulerTaskLauncherException(ERROR_MESSAGE);
	}

	@Test
	public void testMessageAndCause() {
		thrown.expect(SchedulerTaskLauncherException.class);
		thrown.expectMessage(ERROR_MESSAGE);
		NullPointerException expectedCause = new NullPointerException();
		thrown.expectCause(is(expectedCause));
		throw new SchedulerTaskLauncherException(ERROR_MESSAGE, expectedCause);
	}

	@Test
	public void testCause() {
		thrown.expect(SchedulerTaskLauncherException.class);
		NullPointerException expectedCause = new NullPointerException();
		thrown.expectCause(is(expectedCause));
		throw new SchedulerTaskLauncherException(expectedCause);
	}
}
