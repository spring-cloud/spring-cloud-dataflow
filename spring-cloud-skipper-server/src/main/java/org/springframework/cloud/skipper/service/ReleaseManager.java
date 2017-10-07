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
package org.springframework.cloud.skipper.service;

import org.springframework.cloud.skipper.domain.Release;

/**
 * Manages the lifecycle of a releases.
 *
 * The current implementation is a simple sequence of AppDeployer commands, but more
 * sophisticated implementations based on Spring Cloud State Machine are possible.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public interface ReleaseManager {

	Release install(Release release);

	Release upgrade(Release existingRelease, Release replacingRelease, String upgradeStrategyName);

	// Release rollback(Release existingRelease, Release replacingRelease);

	Release delete(Release release);

	Release status(Release release);

}
