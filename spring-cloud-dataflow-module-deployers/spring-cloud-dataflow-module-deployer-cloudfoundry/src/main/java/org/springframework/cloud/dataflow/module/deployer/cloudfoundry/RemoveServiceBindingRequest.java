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
 * @author Eric Bottard
 */
class RemoveServiceBindingRequest {
	private String bindingId;

	private String appId;

	public String getAppId() {
		return appId;
	}

	public RemoveServiceBindingRequest withAppId(String id) {
		this.appId = id;
		return this;
	}

	public String getBindingId() {
		return bindingId;
	}

	public RemoveServiceBindingRequest withBindingId(String bindingId) {
		this.bindingId = bindingId;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) { return true; }
		if (o == null || getClass() != o.getClass()) { return false; }

		RemoveServiceBindingRequest that = (RemoveServiceBindingRequest) o;

		if (bindingId != null ? !bindingId.equals(that.bindingId) : that.bindingId != null) { return false; }
		return !(appId != null ? !appId.equals(that.appId) : that.appId != null);

	}

	@Override
	public int hashCode() {
		int result = bindingId != null ? bindingId.hashCode() : 0;
		result = 31 * result + (appId != null ? appId.hashCode() : 0);
		return result;
	}
}
