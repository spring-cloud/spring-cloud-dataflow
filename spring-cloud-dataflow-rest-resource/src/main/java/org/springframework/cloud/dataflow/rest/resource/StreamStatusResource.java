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

package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;

/**
 * @author Christian Tzolov
 */
public class StreamStatusResource extends RepresentationModel<StreamStatusResource> {

	public static final String SKIPPER_RELEASE_VERSION = "skipper.release.version";
	public static final String NO_APPS = "no apps";
	private String name;

	private CollectionModel<AppStatusResource> applications;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		try {
			if (applications.iterator().hasNext()) {
				CollectionModel<AppInstanceStatusResource> instances = applications.iterator().next().getInstances();
				if (instances != null && instances.iterator().hasNext()) {
					AppInstanceStatusResource instance = instances.iterator().next();
					if (instance != null && instance.getAttributes() != null
							&& instance.getAttributes().containsKey(SKIPPER_RELEASE_VERSION)) {
						String releaseVersion = instance.getAttributes().get(SKIPPER_RELEASE_VERSION);
						if (releaseVersion != null) {
							return releaseVersion;
						}
					}
				}
			}
		}
		catch (Throwable t) {
			// do nothing
		}
		return NO_APPS;
	}

	public CollectionModel<AppStatusResource> getApplications() {
		return applications;
	}

	public void setApplications(CollectionModel<AppStatusResource> applications) {
		this.applications = applications;
	}

	public static class Page extends PagedModel<StreamStatusResource> {

	}
}
