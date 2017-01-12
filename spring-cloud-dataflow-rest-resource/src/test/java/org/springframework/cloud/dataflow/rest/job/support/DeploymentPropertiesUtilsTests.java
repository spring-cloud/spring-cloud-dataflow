/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.rest.job.support;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;

/**
 * Tests for {@link DeploymentPropertiesUtils}.
 *
 * @author Janne Valkealahti
 *
 */
public class DeploymentPropertiesUtilsTests {

	@Test
	public void testDeploymentProperties() {
		assertProps("app.timestamp.foo1=bar1",
				new String[] { "app.timestamp.foo1" },
				new String[] { "bar1" });
		assertProps("app.timestamp.foo1=bar1,app.timestamp.foo2=bar2",
				new String[] { "app.timestamp.foo1", "app.timestamp.foo2" },
				new String[] { "bar1", "bar2" });
	}

	@Test
	public void testCommandLineParamsParsing() {
		assertArrays(new String[] { "--format=yyyy-MM-dd" }, new String[] { "--format=yyyy-MM-dd" });
		assertArrays(new String[] { "'--format=yyyy-MM-dd HH:mm:ss.SSS'" }, new String[] { "--format=yyyy-MM-dd HH:mm:ss.SSS" });
		assertArrays(new String[] { "\"--format=yyyy-MM-dd HH:mm:ss.SSS\"" }, new String[] { "--format=yyyy-MM-dd HH:mm:ss.SSS" });
		assertArrays(new String[] { "--format='yyyy-MM-dd HH:mm:ss.SSS'" }, new String[] { "--format=yyyy-MM-dd HH:mm:ss.SSS" });
		assertArrays(new String[] { "--format=\"yyyy-MM-dd HH:mm:ss.SSS\"" }, new String[] { "--format=yyyy-MM-dd HH:mm:ss.SSS" });
		assertArrays(new String[] { "--foo1=bar1 --foo2=bar2" }, new String[] { "--foo1=bar1", "--foo2=bar2" });
		assertArrays(new String[] { "--foo1=bar1", "--foo2=bar2" }, new String[] { "--foo1=bar1", "--foo2=bar2" });
		assertArrays(new String[] { " --foo1=bar1 ", " --foo2=bar2 " }, new String[] { "--foo1=bar1", "--foo2=bar2" });
		assertArrays(new String[] { "'--format=yyyy-MM-dd HH:mm:ss.SSS'", "--foo1=bar1" },
				new String[] { "--format=yyyy-MM-dd HH:mm:ss.SSS", "--foo1=bar1" });
	}

	private static void assertProps(String left, String[] rightKeys, String[] rightValues) {
		Map<String, String> props = DeploymentPropertiesUtils.parse(left);
		for (int i = 0; i < rightKeys.length; i++) {
			assertThat(props, hasEntry(rightKeys[i], rightValues[i]));
		}
	}

	private static void assertArrays(String[] left, String[] right) {
		ArrayList<String> params = new ArrayList<>(Arrays.asList(left));
		assertThat(DeploymentPropertiesUtils.parseParams(params), containsInAnyOrder(right));
	}
}
