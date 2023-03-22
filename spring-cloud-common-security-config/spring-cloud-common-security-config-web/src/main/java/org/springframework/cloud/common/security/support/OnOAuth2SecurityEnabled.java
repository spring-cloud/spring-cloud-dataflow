/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.common.security.support;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that is only valid if the property
 * {@code security.oauth2.client.client-id} exists.
 *
 * @author Gunnar Hillert
 * @since 1.1.0
 */
public class OnOAuth2SecurityEnabled extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, String> properties = getSubProperties(context.getEnvironment(), "spring.security.oauth2");
		return new ConditionOutcome(!properties.isEmpty(), "OAuth2 Enabled");
	}

	public static Map<String, String> getSubProperties(Environment environment, String keyPrefix) {
		return Binder.get(environment)
			.bind(keyPrefix, Bindable.mapOf(String.class, String.class))
			.orElseGet(Collections::emptyMap);
	}
}
