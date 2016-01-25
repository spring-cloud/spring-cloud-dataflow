/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.admin.controller;

/**
 * Thrown when trying to create some identifiable resource, but one of the same kind and id already exists.
 *
 * @author Eric Bottard
 */
public class ResourceAlreadyExistsException extends RuntimeException {

	/**
	 * The 'name' or id of the resource that already exists and can't be created.
	 */
	private final String name;

	/**
	 * The kind of resource that can't be created.
	 */
	private final String kind;


	public ResourceAlreadyExistsException(String name, String kind, String messageTemplate, Object... args) {
		super(String.format(messageTemplate, args));
		this.name = name;
		this.kind = kind;
	}

	public ResourceAlreadyExistsException(String name, String kind) {
		this(name, kind, "Can't create %s named '%s' because one already exists", kind, name);
	}

	public String getName() {
		return name;
	}

	public String getKind() {
		return kind;
	}
}
