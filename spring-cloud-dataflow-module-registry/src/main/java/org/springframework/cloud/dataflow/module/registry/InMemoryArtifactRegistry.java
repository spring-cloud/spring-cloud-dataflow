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

import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ArtifactType;

/**
 * In-memory implementation of {@link ArtifactRegistry}.
 *
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public class InMemoryArtifactRegistry implements ArtifactRegistry {

	private final Map<Key, ArtifactCoordinates> map = new ConcurrentHashMap<>();

	@Override
	public ArtifactRegistration find(String name, ArtifactType type) {
		ArtifactCoordinates coordinates = this.map.get(new Key(name, type));
		return (coordinates == null ? null : new ArtifactRegistration(name, type, coordinates));
	}

	@Override
	public List<ArtifactRegistration> findAll() {
		List<ArtifactRegistration> list = new ArrayList<>(this.map.size());
		for (Map.Entry<Key, ArtifactCoordinates> entry : this.map.entrySet()) {
			list.add(new ArtifactRegistration(entry.getKey().getName(),
					entry.getKey().getType(), entry.getValue()));
		}
		return list;
	}

	@Override
	public void save(ArtifactRegistration registration) {
		String name = registration.getName();
		ArtifactType type = registration.getType();
		ArtifactCoordinates coordinates = registration.getCoordinates();

		this.map.put(new Key(name, type), coordinates);
	}

	@Override
	public void delete(String name, ArtifactType type) {
		this.map.remove(new Key(name, type));
	}

	/**
	 * Key class for module registration.
	 */
	private static class Key {

		private String name;

		private ArtifactType type;

		public Key(String name, ArtifactType type) {
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public ArtifactType getType() {
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
