/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import java.util.ServiceLoader;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * When Server in not deployed on a cloud platform, it is considered to be on a Local platform.
 *
 * @author Christian Tzolov
 */
public class OnLocalPlatform extends SpringBootCondition {


	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {

		Iterable<CloudProfileProvider> cloudProfileProviders = ServiceLoader.load(CloudProfileProvider.class);
		boolean onLocalPlatform = true;
		for (CloudProfileProvider cloudProfileProvider : cloudProfileProviders) {
			if (cloudProfileProvider.isCloudPlatform(context.getEnvironment())) {
				onLocalPlatform = false;
			}
		}
		if (onLocalPlatform) {
			return new ConditionOutcome(onLocalPlatform, "On local platform.");
		} else {
			return new ConditionOutcome(onLocalPlatform, "On cloud platform.");
		}
	}
}
