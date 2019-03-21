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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.skipper.SkipperException;

/**
 * Default implementation of a {@link ReleaseManagerFactory} returning
 * instances from a context.
 *
 * @author Janne Valkealahti
 *
 */
public class DefaultReleaseManagerFactory implements ReleaseManagerFactory {

	private final Map<String, ReleaseManager> managers = new HashMap<>();

	public DefaultReleaseManagerFactory(List<ReleaseManager> managers) {
		if (managers != null) {
			for (ReleaseManager manager : managers) {
				for (String kind : manager.getSupportedKinds()) {
					this.managers.put(kind, manager);
				}
			}
		}
	}

	@Override
	public ReleaseManager getReleaseManager(String kind) {
		ReleaseManager manager = managers.get(kind);
		if (manager != null) {
			return manager;
		}
		throw new SkipperException("No release manager available for '" + kind + "'");
	}
}
