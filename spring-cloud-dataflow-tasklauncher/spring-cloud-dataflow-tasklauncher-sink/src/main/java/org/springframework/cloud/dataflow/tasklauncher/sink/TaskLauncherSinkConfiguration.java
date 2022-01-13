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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.tasklauncher.TaskLauncherFunction;
import org.springframework.cloud.dataflow.tasklauncher.TaskLauncherFunctionConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.DefaultPollableMessageSource;
import org.springframework.cloud.stream.binder.PollableMessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Configuration class for the TaskLauncher Data Flow Sink.
 *
 * @author David Turanski
 * @author Gunnar Hillert
 */
@EnableBinding(PollingSink.class)
@EnableConfigurationProperties({ TriggerProperties.class })
@Import(TaskLauncherFunctionConfiguration.class)
public class TaskLauncherSinkConfiguration {

	@Value("${autostart:true}")
	private boolean autoStart;

	@Bean
	public DynamicPeriodicTrigger periodicTrigger(TriggerProperties triggerProperties) {
		DynamicPeriodicTrigger trigger = new DynamicPeriodicTrigger(triggerProperties.getPeriod());
		trigger.setInitialDuration(Duration.ofMillis(triggerProperties.getInitialDelay()));
		return trigger;
	}

	/*
	 * For backward compatibility with spring-cloud-stream-2.1.x
	 */
	@Bean
	public BeanPostProcessor addInterceptorToPollableMessageSource() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof DefaultPollableMessageSource) {
					DefaultPollableMessageSource pollableMessageSource = (DefaultPollableMessageSource) bean;
					pollableMessageSource.addInterceptor(new ChannelInterceptor() {
						@Override
						public Message<?> preSend(Message<?> message, MessageChannel channel) {
							Message<?> newMessage = message;
							if (message.getHeaders().containsKey("originalContentType")) {
								newMessage = MessageBuilder.fromMessage(message)
										.setHeader(MessageHeaders.CONTENT_TYPE,
												message.getHeaders().get("originalContentType"))
										.build();
							}
							return newMessage;
						}
					});
				}
				return bean;
			}
		};
	}

	@Bean
	public LaunchRequestConsumer launchRequestConsumer(PollableMessageSource input,
			TaskLauncherFunction taskLauncherFunction, DynamicPeriodicTrigger trigger,
			TriggerProperties triggerProperties) {

		LaunchRequestConsumer consumer = new LaunchRequestConsumer(input,
				trigger, triggerProperties.getMaxPeriod(), taskLauncherFunction);
		consumer.setAutoStartup(autoStart);
		return consumer;
	}
}
