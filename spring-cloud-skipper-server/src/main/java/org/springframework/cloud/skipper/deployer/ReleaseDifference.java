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
package org.springframework.cloud.skipper.deployer;

import org.springframework.util.Assert;

/**
 * @author Mark Pollack
 */
public class ReleaseDifference {

	private final boolean areEqual;

	private final String differenceSummary;

	public ReleaseDifference(boolean areEqual) {
		this(areEqual, "No difference.");
	}

	public ReleaseDifference(boolean areEqual, String differenceSummary) {
		Assert.hasText(differenceSummary, "Difference Summary can not be null.");
		this.areEqual = areEqual;
		this.differenceSummary = differenceSummary;
	}

	boolean areEqual() {
		return this.areEqual;
	}

	public String getDifferenceSummary() {
		return differenceSummary;
	}
}
