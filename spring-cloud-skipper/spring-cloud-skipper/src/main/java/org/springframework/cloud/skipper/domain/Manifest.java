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

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * @author Mark Pollack
 */
@Entity
@Table(name = "SkipperManifest")
public class Manifest extends AbstractEntity {

	@NotNull
	@Lob
	// @Column(columnDefinition = "text")
	private String data;

	public Manifest() {
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Manifest)) {
			return false;
		}

		Manifest manifest = (Manifest) o;

		return data.equals(manifest.data);
	}

	@Override
	public int hashCode() {
		return data.hashCode();
	}
}
