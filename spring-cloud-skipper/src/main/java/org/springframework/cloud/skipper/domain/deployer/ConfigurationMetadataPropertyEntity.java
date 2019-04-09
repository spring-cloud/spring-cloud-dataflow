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
package org.springframework.cloud.skipper.domain.deployer;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

/**
 * This entity class is used to extend boot's {@link ConfigurationMetadataProperty} so that
 * we can add our own constructor and not to explicitly import entity classes from boot.
 *
 * @author Janne Valkealahti
 *
 */
@SuppressWarnings("serial")
public class ConfigurationMetadataPropertyEntity extends ConfigurationMetadataProperty {

	public ConfigurationMetadataPropertyEntity() {
		super();
	}

	public ConfigurationMetadataPropertyEntity(ConfigurationMetadataProperty from) {
		super();
		setId(from.getId());
		setName(from.getName());
		setType(from.getType());
		setDescription(from.getDescription());
		setShortDescription(from.getShortDescription());
		setDefaultValue(from.getDefaultValue());
		getHints().getKeyHints().addAll(from.getHints().getKeyHints());
		getHints().getKeyProviders().addAll(from.getHints().getKeyProviders());
		getHints().getValueHints().addAll(from.getHints().getValueHints());
		getHints().getValueProviders().addAll(from.getHints().getValueProviders());
		setDeprecation(from.getDeprecation());
	}
}
