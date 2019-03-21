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

/**
 * This contains all the response attributes for cancel operation.
 *
 * @author Janne Valkealahti
 *
 */
public class CancelResponse {

	private Boolean accepted;

	public CancelResponse() {
	}

	public CancelResponse(Boolean accepted) {
		this.accepted = accepted;
	}

	public Boolean getAccepted() {
		return accepted;
	}

	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}

	@Override
	public String toString() {
		return "CancelResponse [accepted=" + accepted + "]";
	}
}
