/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.deployer;

/**
 * Interface resolving {@link ReleaseManager} for an {@code application kind}.
 *
 * @author Janne Valkealahti
 *
 */
public interface ReleaseManagerFactory {

	/**
	 * Resolve {@link ReleaseManager} for an {@code application kind}.
	 *
	 * @param kind the application kind
	 * @return the resolved released manager
	 */
	ReleaseManager getReleaseManager(String kind);
}
