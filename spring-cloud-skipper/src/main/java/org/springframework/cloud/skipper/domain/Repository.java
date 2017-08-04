/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.domain;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * @author Mark Pollack
 */
@Entity
public class Repository {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	/**
	 * A short name, e.g. 'stable' to associate with this repository, must be unique.
	 */
	@NotNull
	@Column(unique = true)
	private String name;

	/**
	 * The root url that points to the location of an index.yaml file and other files
	 * supporting the index e.g. icons-64x64.zip
	 */
	@NotNull
	private String url;

	// TODO security/checksum fields of referenced index file.

	public Repository() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
