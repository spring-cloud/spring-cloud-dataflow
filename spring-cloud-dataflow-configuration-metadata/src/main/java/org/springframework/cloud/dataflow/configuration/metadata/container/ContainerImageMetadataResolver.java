/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata.container;

import java.util.Map;

/**
 * Retrieves the configuration metadata for Docker and OCI container images.
 *
 * @author Christian Tzolov
 */
public interface ContainerImageMetadataResolver {
	/**
	 * @param imageName container image name
	 * @return Returns map of all image configuration labels.
	 */
	Map<String, String> getImageLabels(String imageName);

	/**
	 * @param imageName container image name
	 * @return Returns image's configuration object structured as nested Maps.
	 */
	Map<String, Object> getImageConfig(String imageName);

	/**
	 * @param imageName container image name
	 * @return Returns the image's manifest as JSON.
	 */
	String getImageManifest(String imageName);

	/**
	 * @param imageName container image name
	 * @return Returns all image tags.
	 */
	String[] getImageTags(String imageName);
}
