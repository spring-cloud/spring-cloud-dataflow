/*
 * Copyright 2017-2018 the original author or authors.
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

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import org.springframework.cloud.dataflow.common.persistence.type.DatabaseAwareLobUserType;


/**
 * Repository for the packages.
 *
 * @author Mark Pollack
 * @author Gunnar Hillert
 *
 */
@Entity
@Table(name = "SkipperRepository", uniqueConstraints = @UniqueConstraint(name = "uk_repository", columnNames = {
		"name" }), indexes = @Index(name = "idx_repo_name", columnList = "name"))
public class Repository extends AbstractEntity {

	/**
	 * A short name, e.g. 'stable' to associate with this repository, must be unique.
	 */
	@NotNull
	private String name;

	/**
	 * The root url that points to the location of an index.yaml file and other files
	 * supporting the index e.g. myapp-1.0.0.zip, icons-64x64.zip
	 */
	@NotNull
	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String url;

	/**
	 * The url that points to the source package files that was used to create the index and
	 * packages.
	 */
	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String sourceUrl;

	/**
	 * Is this a local or remote repository. Uploads are only allowed to a local repository
	 */
	private Boolean local = false;

	/**
	 * A short description of the repository.
	 */
	private String description;

	/**
	 * An integer used to determine which repository is preferred over others when searching
	 * for a package.
	 */
	private Integer repoOrder;

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

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getRepoOrder() {
		return repoOrder;
	}

	public void setRepoOrder(Integer repoOrder) {
		this.repoOrder = repoOrder;
	}

	@Override
	public String toString() {
		return "Repository{" +
				"name='" + name + '\'' +
				", url='" + url + '\'' +
				", local=" + local +
				'}';
	}
}
