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

package org.springframework.cloud.dataflow.module.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cloud.dataflow.core.ModuleCoordinates;
import org.springframework.cloud.dataflow.core.ModuleType;

/**
 * In-memory implementation of {@link ModuleRegistry}.
 *
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public class InMemoryModuleRegistry implements ModuleRegistry {

	private final Map<Key, ModuleCoordinates> map = new ConcurrentHashMap<>();

	@Override
	public ModuleRegistration find(String name, ModuleType type) {
		ModuleCoordinates coordinates = this.map.get(new Key(name, type));
		return (coordinates == null ? null : new ModuleRegistration(name, type, coordinates));
	}

	@Override
	public List<ModuleRegistration> findAll() {
		List<ModuleRegistration> list = new ArrayList<>(this.map.size());
		for (Map.Entry<Key, ModuleCoordinates> entry : this.map.entrySet()) {
			list.add(new ModuleRegistration(entry.getKey().getName(),
					entry.getKey().getType(), entry.getValue()));
		}
		return list;
	}

	@Override
	public void save(ModuleRegistration registration) {
		String name = registration.getName();
		ModuleType type = registration.getType();
		ModuleCoordinates coordinates = registration.getCoordinates();

		this.map.put(new Key(name, type), coordinates);
	}

	@Override
	public void delete(String name, ModuleType type) {
		this.map.remove(new Key(name, type));
	}

	/**
	 * Key class for module registration.
	 */
	private static class Key {

		private String name;

		private ModuleType type;

		public Key(String name, ModuleType type) {
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public ModuleType getType() {
			return type;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Key that = (Key) o;
			return this.getName().equals(that.getName()) && this.getType() == that.getType();
		}

		@Override
		public int hashCode() {
			int result = name.hashCode();
			result = 31 * result + type.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return "Key{" +
					"name='" + name + '\'' +
					", type=" + type +
					'}';
		}
	}

}
