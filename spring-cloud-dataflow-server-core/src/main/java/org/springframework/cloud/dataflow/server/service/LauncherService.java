/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service;

import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Offers services to retrieve the launchers that are available for the
 * Spring Cloud Data Flow instance.
 *
 * @author Glenn Renfro
 */
public interface LauncherService {

	/**
	 * Retrieve all launchers that are available.
	 * @param pageable how the results should be returned.
	 * @return a Page containing the available launchers.
	 */
	public Page<Launcher> getAllLaunchers(Pageable pageable);

	/**
	 * Retrieve all launchers that are configured to a platform scheduler.
	 * @param pageable how the results should be returned.
	 * @return a Page containing the available launchers.
	 */
	public Page<Launcher> getLaunchersWithSchedules(Pageable pageable) ;
}
