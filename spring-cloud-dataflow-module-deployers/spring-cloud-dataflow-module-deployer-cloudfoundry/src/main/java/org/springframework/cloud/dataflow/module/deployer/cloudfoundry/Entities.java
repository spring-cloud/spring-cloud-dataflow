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

package org.springframework.cloud.dataflow.module.deployer.cloudfoundry;

/**
 * The Entity types used in the REST response types.
 * @author Steve Powell
 */
class Entities {
	/**
	 * Base class.
	 */
	static class BaseEntity {
	}

	/**
	 * A named entity with state.
	 */
	static class ApplicationEntity extends NamedEntity<ApplicationEntity> {
		private volatile String state;

		public String getState() {
			return state;
		}

		public ApplicationEntity withState(String state) {
			this.state = state;
			return this;
		}
	}

	/**
	 * An entity with a name.
	 */
	static class NamedEntity<E extends NamedEntity<E>> extends BaseEntity {

		private volatile String name;

		public String getName() {
			return name;
		}

		public E withName(String name) {
			this.name = name;
			@SuppressWarnings("unchecked") E ethis = (E) this;
			return ethis;
		}
	}

	/**
	 * An entity with route information.
	 */
	static class RouteEntity extends BaseEntity {

		private String host;

		private String path;

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
	}

	/**
	 * Entity in a service binding response.
	 */
	static class ServiceBindingEntity extends BaseEntity {
	}
}
