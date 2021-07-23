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

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.tasklauncher.LaunchRequest;
import org.springframework.cloud.dataflow.tasklauncher.TaskLauncherFunction;
import org.springframework.cloud.stream.binder.PollableMessageSource;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.util.Assert;

/**
 *
 * A Message consumer that submits received task {@link LaunchRequest}s to a Data Flow
 * server. This polls a {@link PollableMessageSource} only if the Data Flow server is not
 * at its concurrent task execution limit.
 *
 * The consumer runs as a {@link ScheduledFuture} , configured with a
 * {@link DynamicPeriodicTrigger} to support exponential backoff up to a maximum period.
 * Every period cycle, the poller first makes a REST call to the Data Flow server to check
 * if it can accept a new task LaunchRequest before checking the Message source. The
 * polling period will back off (increase) when either the server is not accepting
 * requests or no request is received.
 *
 * The period will revert to its initial value whenever both a request is received and the
 * DataFlow Server is accepting launch requests. The period remain at the maximum value
 * when there are no requests to avoid hammering the Data Flow server for no reason.
 *
 * @author David Turanski
 **/
public class LaunchRequestConsumer implements SmartLifecycle {
	private static final Log log = LogFactory.getLog(LaunchRequestConsumer.class);

	private static final int BACKOFF_MULTIPLE = 2;

	static final String TASK_PLATFORM_NAME = "spring.cloud.dataflow.task.platformName";

	private final PollableMessageSource input;

	private final AtomicBoolean running = new AtomicBoolean();

	private final AtomicBoolean paused = new AtomicBoolean();

	private final DynamicPeriodicTrigger trigger;

	private final ConcurrentTaskScheduler taskScheduler;

	private final long initialPeriod;

	private final long maxPeriod;

	private volatile boolean autoStart = true;

	private final TaskLauncherFunction taskLauncherFunction;

	private ScheduledFuture<?> scheduledFuture;

	public LaunchRequestConsumer(PollableMessageSource input, DynamicPeriodicTrigger trigger,
			long maxPeriod, TaskLauncherFunction taskLauncherFunction) {
		Assert.notNull(input, "`input` cannot be null.");
		Assert.notNull(taskLauncherFunction, "`taskLauncherFunction` cannot be null.");
		this.taskLauncherFunction = taskLauncherFunction;
		this.input = input;
		this.trigger = trigger;
		this.initialPeriod = trigger.getDuration().toMillis();
		this.maxPeriod = maxPeriod;
		this.taskScheduler = new ConcurrentTaskScheduler();
	}

	/*
	 * Polling loop
	 */
	ScheduledFuture<?> consume() {

		return taskScheduler.schedule(() -> {
			if (!isRunning()) {
				return;
			}

			if (taskLauncherFunction.platformIsAcceptingNewTasks()) {
				if (paused.compareAndSet(true, false)) {
					log.info("Polling resumed");
				}

				if (!input.poll(message -> {
					LaunchRequest request = (LaunchRequest) message.getPayload();
					log.debug("Received a Task launch request - task name:  " + request.getTaskName());
					taskLauncherFunction.apply(request);
				}, new ParameterizedTypeReference<LaunchRequest>() {
				})) {
					backoff("No task launch request received");
				}
				else {
					if (trigger.getDuration().toMillis() > initialPeriod) {
						trigger.setDuration(Duration.ofMillis(initialPeriod));
						log.info(String.format("Polling period reset to %d ms.", trigger.getDuration().toMillis()));
					}
				}
			}
			else {
				paused.set(true);
				backoff("Polling paused");

			}
		}, trigger);
	}

	@Override
	public boolean isAutoStartup() {
		return autoStart;
	}

	public void setAutoStartup(boolean autoStart) {
		this.autoStart = autoStart;
	}

	@Override
	public synchronized void stop(Runnable callback) {
		if (callback != null) {
			callback.run();
		}
		this.stop();
	}

	@Override
	public void start() {
		if (running.compareAndSet(false, true)) {
			this.scheduledFuture = consume();
		}
	}

	@Override
	public void stop() {
		if (running.getAndSet(false)) {
			this.scheduledFuture.cancel(false);
		}
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

	public boolean isPaused() {
		return paused.get();
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	private void backoff(String message) {
		synchronized (trigger) {
			if (trigger.getDuration().compareTo(Duration.ZERO) > 0
					&& trigger.getDuration().compareTo(Duration.ofMillis(maxPeriod)) < 0) {

				Duration duration = trigger.getDuration();

				if (duration.multipliedBy(BACKOFF_MULTIPLE).compareTo(Duration.ofMillis(maxPeriod)) <= 0) {
					// If d >= 1, round to 1 seconds.
					if (duration.getSeconds() == 1) {
						duration = Duration.ofSeconds(1);
					}
					duration = duration.multipliedBy(BACKOFF_MULTIPLE);
				}
				else {
					duration = Duration.ofMillis(maxPeriod);
				}
				if (trigger.getDuration().toMillis() < 1000) {
					log.info(String.format(message + " - increasing polling period to %d ms.", duration.toMillis()));
				}
				else {
					log.info(
							String.format(message + "- increasing polling period to %d seconds.",
									duration.getSeconds()));
				}

				trigger.setDuration(duration);
			}
			else if (trigger.getDuration() == Duration.ofMillis(maxPeriod)) {
				log.info(message);
			}
		}
	}

}
