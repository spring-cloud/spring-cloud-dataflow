/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.module.registry;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.data.core.ModuleCoordinates;

/**
 * @author Mark Fisher
 */
public class InMemoryModuleRegistry implements ModuleRegistry {

	private final Map<String, ModuleCoordinates> sources = new HashMap<>();

	private final Map<String, ModuleCoordinates> processors = new HashMap<>();

	private final Map<String, ModuleCoordinates> sinks = new HashMap<>();

	@Override
	public ModuleCoordinates findByNameAndType(String name, String type) {
		if ("source".equals(type)) {
			return sources.get(name);
		}
		if ("processor".equals(type)) {
			return processors.get(name);
		}
		if ("sink".equals(type)) {
			return sinks.get(name);
		}
		throw new UnsupportedOperationException("only 'source', 'processor', and 'sink' types are supported");
	}

	@Override
	public void save(String name, String type, ModuleCoordinates coordinates) {
		if ("source".equals(type)) {
			this.sources.put(name, coordinates);
		}
		if ("processor".equals(type)) {
			this.processors.put(name, coordinates);
		}
		if ("sink".equals(type)) {
			this.sinks.put(name, coordinates);
		}
	}
}
