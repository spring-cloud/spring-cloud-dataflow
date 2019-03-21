/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.server.domain;

import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.data.rest.core.config.Projection;

/**
 * Defines summary information of a package, only the id, name, version and icon URL.
 *
 * Interface that adds the Spring Data REST Projection annotation to avoid adding a Spring
 * Data REST dependency to the core skipper domain class.
 *
 * NOTE: Projection interfaces need to be in the same Java package as the core domain
 * model entity.
 * @author Mark Pollack
 */
@Projection(name = "summary", types = { PackageMetadata.class })
public interface PackageSummary {

	String getId();

	String getName();

	String getVersion();

	String getIconUrl();

	String getDescription();

	String getRepositoryName();
}
