/*
 * Copyright 2022 the original author or authors.
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

import java.util.Map;
import java.util.Objects;

import org.springframework.util.Assert;

public class ActuatorPostRequest {

	private String endpoint;

	private Map<String, Object> body;

	/**
	 * @author David Turanski
	 */
	public ActuatorPostRequest() {
	}

	/**
	 * @param endpoint the relative actuator path, e.g., {@code /info}, base actuator url if empty.
	 * @param body the request body as JSON text
	 * @return
	 */
	public static ActuatorPostRequest of(String endpoint, Map<String, Object> body) {
		Assert.notEmpty(body, "'body' must not be empty");
		ActuatorPostRequest actuatorPostRequest = new ActuatorPostRequest();
		actuatorPostRequest.setEndpoint(endpoint == null ? "/" : endpoint);
		actuatorPostRequest.setBody(body);
		return actuatorPostRequest;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Map<String, Object> getBody() {
		return body;
	}

	public void setBody(Map<String, Object> body) {
		this.body = body;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ActuatorPostRequest that = (ActuatorPostRequest) o;
		return Objects.equals(endpoint, that.endpoint) && Objects.equals(body, that.body);
	}

	@Override
	public int hashCode() {
		return Objects.hash(endpoint, body);
	}
}
