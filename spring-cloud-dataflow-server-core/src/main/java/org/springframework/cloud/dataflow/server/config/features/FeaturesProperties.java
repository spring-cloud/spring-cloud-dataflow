/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.features;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;

/**
 * Configuration properties for all the features that need to be enabled/disabled at the
 * dataflow server.
 *
 * @author Ilayaperumal Gopinathan
 */
@ConfigurationProperties(prefix = FeaturesProperties.FEATURES_PREFIX)
public class FeaturesProperties {

	public static final String FEATURES_PREFIX = DataFlowPropertyKeys.PREFIX + "features";

	public static final String STREAMS_ENABLED = "streams-enabled";

	public static final String SCHEDULES_ENABLED = "schedules-enabled";

	public static final String TASKS_ENABLED = "tasks-enabled";

	public static final String UNDERSCORE_NAMES_ENABLED = "underscore-names-enabled";

	private boolean streamsEnabled = true;

	private boolean tasksEnabled = true;

	private boolean schedulesEnabled = false;

	private boolean underscoreNamesEnabled = false;

	public boolean isStreamsEnabled() {
		return this.streamsEnabled;
	}

	public void setStreamsEnabled(boolean streamsEnabled) {
		this.streamsEnabled = streamsEnabled;
	}

	public boolean isTasksEnabled() {
		return this.tasksEnabled;
	}

	public void setTasksEnabled(boolean tasksEnabled) {
		this.tasksEnabled = tasksEnabled;
	}

	public boolean isSchedulesEnabled() {
		return schedulesEnabled;
	}

	public void setSchedulesEnabled(boolean schedulesEnabled) {
		this.schedulesEnabled = schedulesEnabled;
	}

	public boolean isUnderscoreNamesEnabled() {
		return underscoreNamesEnabled;
	}

	public void setUnderscoreNamesEnabled(boolean underscoreNamesEnabled) {
		this.underscoreNamesEnabled = underscoreNamesEnabled;
	}
}
