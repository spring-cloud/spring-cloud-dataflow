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
 * @author Christian Tzolov
 */
public class DeleteProperties {

	private boolean deletePackage;

	public boolean isDeletePackage() {
		return deletePackage;
	}

	public void setDeletePackage(boolean deletePackage) {
		this.deletePackage = deletePackage;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("DeleteProperties{");
		sb.append("deletePackage=").append(deletePackage);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DeleteProperties)) {
			return false;
		}

		DeleteProperties that = (DeleteProperties) o;

		return deletePackage == that.deletePackage;
	}

	@Override
	public int hashCode() {
		return (deletePackage ? 1 : 0);
	}
}
