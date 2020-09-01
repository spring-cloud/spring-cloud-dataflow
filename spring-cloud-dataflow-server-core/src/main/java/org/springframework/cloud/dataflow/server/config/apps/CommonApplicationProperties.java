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

package org.springframework.cloud.dataflow.server.config.apps;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Common properties for applications deployed via Spring Cloud Data Flow.
 *
 * If you want to pass a property placeholder to the downstream applications use the <pre>^$^{...}</pre> syntax
 * instead of <pre>${...}</pre>. The <pre>${...}</pre> placeholders are resolved by the Spring Cloud Data Flow itself
 * and the <pre>^$^{...}</pre> placeholders are pass through to the deployed apps are resolved there.
 *
 * @author Marius Bogoevici
 * @author Janne Valkealahti
 * @author Christian Tzolov
 */
@ConfigurationProperties(DataFlowPropertyKeys.PREFIX + "application-properties")
public class CommonApplicationProperties {

	public static final String PASSTHROUGH_PLACEHOLDER_PREFIX = "^$^{";

	private Map<String, String> stream = new ConcurrentHashMap<>();
	private Map<String, String> task = new ConcurrentHashMap<>();

	public Map<String, String> getStream() {
		return this.stream;
	}

	public void setStream(Map<String, String> stream) {
		this.stream = stream;
	}

	public Map<String, String> getTask() {
		return this.task;
	}

	public void setTask(Map<String, String> task) {
		this.task = task;
	}

	public Map<String, String> getStreamDecoded() {
		return passThroughPlaceholderDecoder(this.getStream());
	}

	public Map<String, String> getTaskDecoded() {
		return passThroughPlaceholderDecoder(this.getTask());
	}

	/**
	 * If the property value has a placeholder starting with the PASS_THROUGH_PROPERTY_PLACEHOLDER_PREFIX  prefix,
	 * this utility helps to convert it back into a Spring property placeholder prefix before passing it to the
	 * deployed applications.
	 * @param commonAppProperties Common application properties.
	 * @return Returns the input common properties but with pass-through placeholders <pre>^$^{...}</pre> converted to
	 * Spring placeholders: <pre>${...}</pre>.
	 */
	public static Map<String, String> passThroughPlaceholderDecoder(Map<String, String> commonAppProperties) {
		return CollectionUtils.isEmpty(commonAppProperties) ? commonAppProperties :
				commonAppProperties.entrySet().stream().collect(
						Collectors.toMap(Map.Entry::getKey, e -> StringUtils.hasText(e.getValue()) ?
								e.getValue().replace(PASSTHROUGH_PLACEHOLDER_PREFIX,
										PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX) : ""));
	}

}
