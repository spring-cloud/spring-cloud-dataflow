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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Holds dependency information of a libraries used by Spring Cloud Skipper.
 *
 * @author Janne Valkealahti
 *
 */
public class Dependency {

	private String name;

	private String version;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String checksumSha1;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String checksumSha256;


	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String url;

	/**
	 * Default constructor for serialization frameworks.
	 */
	public Dependency() {
	}

	public Dependency(String name, String version, String checksumsha1,
			String checksumsha256, String url) {
		super();
		this.name = name;
		this.version = version;
		this.checksumSha1 = checksumsha1;
		this.checksumSha256 = checksumsha256;
		this.url = url;
	}

	/**
	 * Retrieve the current name for the {@link Dependency}
	 * @return the name for the {@link Dependency}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Establish the name for the {@link Dependency}.
	 *
	 * @param name {@link String} representing the name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Retrieve the current version for the {@link Dependency}
	 *
	 * @return the version for the {@link Dependency}
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Establish the version for the {@link Dependency}.
	 *
	 * @param version {@link String} representing the version.
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Retrieve the current checksumSha1 for the {@link Dependency}
	 *
	 * @return the checksumSha1 for the {@link Dependency}
	 */
	public String getChecksumSha1() {
		return checksumSha1;
	}

	/**
	 * Establish the checksumSha1 for the {@link Dependency}.
	 *
	 * @param checksumSha1 {@link String} representing the checksumSha1.
	 */
	public void setChecksumSha1(String checksumSha1) {
		this.checksumSha1 = checksumSha1;
	}

	/**
	 * Retrieve the current url for the {@link Dependency}
	 *
	 * @return the url for the {@link Dependency}
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Establish the url for the {@link Dependency}.
	 *
	 * @param url {@link String} representing the url.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Retrieve the current checksumSha256 for the {@link Dependency}
	 *
	 * @return the checksumSha256 for the {@link Dependency}
	 */
	public String getChecksumSha256() {
		return checksumSha256;
	}

	/**
	 * Establish the checksumSha1256 for the {@link Dependency}.
	 *
	 * @param checksumSha256 {@link String} representing the checksumSha256.
	 */
	public void setChecksumSha256(String checksumSha256) {
		this.checksumSha256 = checksumSha256;
	}
}
