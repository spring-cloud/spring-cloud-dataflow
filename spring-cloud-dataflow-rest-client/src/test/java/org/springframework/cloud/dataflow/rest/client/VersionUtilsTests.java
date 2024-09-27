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
package org.springframework.cloud.dataflow.rest.client;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.rest.client.support.VersionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
class VersionUtilsTests {

	@Test
	void nullAndBlank() {
		String threePartVersion = VersionUtils.getThreePartVersion(null);
		assertThat(threePartVersion).isEmpty();

		threePartVersion = VersionUtils.getThreePartVersion("");
		assertThat(threePartVersion).isEmpty();

		assertThat(VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion(null,null)).isFalse();
		assertThat(VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion("","")).isFalse();
	}

	@Test
	void badFormat() {
		String threePartVersion = VersionUtils.getThreePartVersion("1.3");
		assertThat(threePartVersion).isEmpty();

		threePartVersion = VersionUtils.getThreePartVersion("1.3.4.5");
		assertThat(threePartVersion);

		threePartVersion = VersionUtils.getThreePartVersion("1.3.4");
		assertThat(threePartVersion).isEqualTo("1.3.4");

		assertThat(VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion("1.3","1.5")).isFalse();
		assertThat(VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion("1.4.5.4","1.5")).isFalse();
		assertThat(VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion("1.5","1.5.3.4")).isFalse();
	}

	@Test
	void valid() {

		String threePartVersion = VersionUtils.getThreePartVersion("1.3.4");
		assertThat(threePartVersion).isEqualTo("1.3.4");

		threePartVersion = VersionUtils.getThreePartVersion("1.3.4.6");
		assertThat(threePartVersion).isEqualTo("1.3.4");

		assertThat(VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion("1.6.0","1.6.0")).isTrue();
		assertThat(VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion("1.7.0","1.6.0")).isTrue();
		assertThat(VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion("1.6.0","1.7.0")).isFalse();
	}

}
