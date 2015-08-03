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

package org.springframework.cloud.data.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.data.core.StreamDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * @author Mark Fisher
 */
public class StubStreamDefinitionRepository implements StreamDefinitionRepository {

	private final Map<String, StreamDefinition> definitions = new HashMap<>();

	@Override
	public Iterable<StreamDefinition> findAll(Sort sort) {
		// todo: figure out sorting
		return Collections.unmodifiableCollection(definitions.values());
	}

	@Override
	public Page<StreamDefinition> findAll(Pageable pageable) {
		// todo: figure out paging
		return new PageImpl<StreamDefinition>(new ArrayList<>(definitions.values()));
	}

	@Override
	public <S extends StreamDefinition> Iterable<S> save(Iterable<S> entities) {
		for (S definition : entities) {
			save(definition);
		}
		return entities;
	}

	@Override
	public <S extends StreamDefinition> S save(S definition) {
		definitions.put(definition.getName(), definition);
		return definition;
	}

	@Override
	public StreamDefinition findOne(String s) {
		return definitions.get(s);
	}

	@Override
	public boolean exists(String s) {
		return definitions.containsKey(s);
	}

	@Override
	public Iterable<StreamDefinition> findAll() {
		return Collections.unmodifiableCollection(definitions.values());
	}

	@Override
	public Iterable<StreamDefinition> findAll(Iterable<String> strings) {
		List<StreamDefinition> results = new ArrayList<>();
		for (String s : strings) {
			if (definitions.containsKey(s)){
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
	public void delete(String s) {
		definitions.remove(s);
	}

	@Override
	public void delete(StreamDefinition entity) {
		delete(entity.getName());
	}

	@Override
	public void delete(Iterable<? extends StreamDefinition> entities) {
		for (StreamDefinition definition : entities){
			delete(definition);
		}
	}

	@Override
	public void deleteAll() {
		definitions.clear();
	}

}
