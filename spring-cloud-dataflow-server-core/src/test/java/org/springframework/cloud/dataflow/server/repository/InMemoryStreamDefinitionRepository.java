/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.server.repository.support.SearchPageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * In-memory implementation of {@link StreamDefinitionRepository}.
 *
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public class InMemoryStreamDefinitionRepository implements StreamDefinitionRepository {

	private final Map<String, StreamDefinition> definitions = new ConcurrentHashMap<>();

	@Override
	public Iterable<StreamDefinition> findAll(Sort sort) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Page<StreamDefinition> findAll(Pageable pageable) {
		List<StreamDefinition> results = new ArrayList<>(definitions.values());
		return new PageImpl<>(results, pageable, results.size());
	}

	@Override
	public <S extends StreamDefinition> Iterable<S> save(Iterable<S> iterableDefinitions) {
		for (S definition : iterableDefinitions) {
			save(definition);
		}
		return iterableDefinitions;
	}

	@Override
	public <S extends StreamDefinition> S save(S definition) {
		if (definitions.containsKey(definition.getName())) {
			throw new DuplicateTaskException(
					String.format("Cannot register stream definition %s because another one has already "
							+ "been registered with the same name", definition.getName()));
		}
		definitions.put(definition.getName(), definition);
		return definition;
	}

	@Override
	public StreamDefinition findOne(String name) {
		return definitions.get(name);
	}

	@Override
	public boolean exists(String name) {
		return definitions.containsKey(name);
	}

	@Override
	public Iterable<StreamDefinition> findAll() {
		return Collections.unmodifiableCollection(definitions.values());
	}

	@Override
	public Iterable<StreamDefinition> findAll(Iterable<String> names) {
		List<StreamDefinition> results = new ArrayList<>();
		for (String s : names) {
			if (definitions.containsKey(s)) {
				results.add(definitions.get(s));
			}
		}
		return results;
	}

	@Override
	public long count() {
		return definitions.size();
	}

	@Override
	public void delete(String name) {
		definitions.remove(name);
	}

	@Override
	public void delete(StreamDefinition definition) {
		delete(definition.getName());
	}

	@Override
	public void delete(Iterable<? extends StreamDefinition> definitions) {
		for (StreamDefinition definition : definitions) {
			delete(definition);
		}
	}

	@Override
	public void deleteAll() {
		definitions.clear();
	}

	@Override
	public Page<StreamDefinition> search(SearchPageable searchPageable) {
		throw new UnsupportedOperationException();
	}

}
