/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.domain.deployer;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the difference between two releaes.
 * @author Mark Pollack
 */
public class ReleaseDifference {

	private List<ApplicationManifestDifference> differences;

	public ReleaseDifference() {
	}

	public List<ApplicationManifestDifference> getDifferences() {
		return differences;
	}

	public void setDifferences(List<ApplicationManifestDifference> differences) {
		this.differences = differences;
	}

	public boolean areEqual() {
		boolean areEqual = true;
		for (ApplicationManifestDifference applicationManifestDifference : differences) {
			if (!applicationManifestDifference.areEqual()) {
				areEqual = false;
				break;
			}
		}
		return areEqual;
	}

	public List<String> getChangedApplicationNames() {
		List<String> names = new ArrayList<>();
		for (ApplicationManifestDifference applicationManifestDifference : differences) {
			if (!applicationManifestDifference.areEqual()) {
				names.add(applicationManifestDifference.getApplicationName());
			}
		}
		return names;
	}

}
