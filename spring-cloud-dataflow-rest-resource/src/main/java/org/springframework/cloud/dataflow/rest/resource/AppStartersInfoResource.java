/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.ResourceSupport;

/**
 *
 * Provides information (uris) for App Starters.
 *
 * @author Gunnar Hillert
 */
public class AppStartersInfoResource extends ResourceSupport {

	/**
	 * Default constructor for serialization frameworks.
	 */
	public AppStartersInfoResource() {
	}

	private List<AppStarter> streamAppStarters = new ArrayList<>(0);
	private List<AppStarter> taskAppStarters = new ArrayList<>(0);

	public List<AppStarter> getStreamAppStarters() {
		return streamAppStarters;
	}
	public void setStreamAppStarters(List<AppStarter> streamAppStarters) {
		this.streamAppStarters = streamAppStarters;
	}
	public List<AppStarter> getTaskAppStarters() {
		return taskAppStarters;
	}
	public void setTaskAppStarters(List<AppStarter> taskAppStarters) {
		this.taskAppStarters = taskAppStarters;
	}

}
