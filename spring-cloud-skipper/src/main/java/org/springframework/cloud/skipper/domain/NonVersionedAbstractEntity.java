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
package org.springframework.cloud.skipper.domain;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.hateoas.Identifiable;

/**
 * Base class for entity implementations that don't need optimistic locking.
 * Uses a {@link Long} id.
 *
 * @author Glenn Renfro
 */
@MappedSuperclass
public class NonVersionedAbstractEntity implements Identifiable<Long> {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private final Long id;

	protected NonVersionedAbstractEntity() {
		this.id = null;
	}

	@Override
	public Long getId() {
		return this.id;
	}

}
