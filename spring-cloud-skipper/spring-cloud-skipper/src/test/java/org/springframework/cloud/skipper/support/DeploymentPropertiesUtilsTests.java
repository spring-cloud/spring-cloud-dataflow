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

package org.springframework.cloud.skipper.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DeploymentPropertiesUtils}.
 *
 * @author Janne Valkealahti
 * @author Corneil du Plessis
 */
class DeploymentPropertiesUtilsTests {

	private static void assertArrays(String[] left, String[] right) {
		ArrayList<String> params = new ArrayList<>(Arrays.asList(left));
		assertThat(DeploymentPropertiesUtils.parseParams(params)).containsExactlyInAnyOrder(right);
	}

	@Test
	void deploymentPropertiesParsing() {
		Map<String, String> props = DeploymentPropertiesUtils.parse("app.foo.bar=v, app.foo.wizz=v2  , deployer.foo"
				+ ".pot=fern, app.other.key = value  , deployer.other.cow = meww");

		assertThat(props)
				.containsEntry("app.foo.bar", "v")
				.containsEntry("app.other.key", "value")
				.containsEntry("app.foo.wizz", "v2")
				.containsEntry("deployer.foo.pot", "fern")
				.containsEntry("deployer.other.cow", "meww");

		props = DeploymentPropertiesUtils.parse("f=v");
		assertThat(props).containsEntry("f", "v");

		props = DeploymentPropertiesUtils.parse("foo1=bar1,app.foo2=bar2,foo3=bar3,xxx3");
		assertThat(props)
				.containsEntry("foo1", "bar1")
				.containsEntry("app.foo2", "bar2")
				.containsEntry("foo3", "bar3,xxx3");

		props = DeploymentPropertiesUtils.parse("foo1 = bar1 , app.foo2= bar2,  foo3  = bar3,xxx3");
		assertThat(props)
				.containsEntry("foo1", "bar1")
				.containsEntry("app.foo2", "bar2")
				.containsEntry("foo3", "bar3,xxx3");

		props = DeploymentPropertiesUtils.parse("app.*.count=1");
		assertThat(props).containsEntry("app.*.count", "1");

		props = DeploymentPropertiesUtils.parse("app.*.my-count=1");
		assertThat(props).containsEntry("app.*.my-count", "1");

		props = DeploymentPropertiesUtils.parse("app.transform.producer.partitionKeyExpression=fakeExpression('xxx')");
		assertThat(props).containsEntry("app.transform.producer.partitionKeyExpression", "fakeExpression('xxx')");

		props = DeploymentPropertiesUtils.parse("invalidkeyvalue");
		assertThat(props).isEmpty();

		props = DeploymentPropertiesUtils.parse("invalidkeyvalue1,invalidkeyvalue2");
		assertThat(props).isEmpty();

		props = DeploymentPropertiesUtils.parse("invalidkeyvalue1,invalidkeyvalue2,foo=bar");
		assertThat(props)
				.hasSize(1)
				.containsEntry("foo", "bar");

		props = DeploymentPropertiesUtils.parse("invalidkeyvalue1,foo=bar,invalidkeyvalue2");
		assertThat(props)
				.hasSize(1)
				.containsEntry("foo", "bar,invalidkeyvalue2");

		props = DeploymentPropertiesUtils.parse("foo.bar1=jee1,jee2,jee3,foo.bar2=jee4,jee5,jee6");
		assertThat(props)
				.containsEntry("foo.bar1", "jee1,jee2,jee3")
				.containsEntry("foo.bar2", "jee4,jee5,jee6");

		props = DeploymentPropertiesUtils.parse("foo.bar1=xxx=1,foo.bar2=xxx=2");
		assertThat(props)
				.containsEntry("foo.bar1", "xxx=1")
				.containsEntry("foo.bar2", "xxx=2");
	}

	@Test
	void longDeploymentPropertyValues() {
		Map<String, String> props = DeploymentPropertiesUtils
				.parse("app.foo.bar=FoooooooooooooooooooooBar,app.foo" + ".bar2=FoooooooooooooooooooooBar");
		assertThat(props).containsEntry("app.foo.bar", "FoooooooooooooooooooooBar");
		props = DeploymentPropertiesUtils.parse("app.foo.bar=FooooooooooooooooooooooooooooooooooooooooooooooooooooBar");
		assertThat(props).containsEntry("app.foo.bar", "FooooooooooooooooooooooooooooooooooooooooooooooooooooBar");
	}

	@Test
	void commandLineParamsParsing() {
		assertArrays(new String[] { "--format=yyyy-MM-dd" }, new String[] { "--format=yyyy-MM-dd" });
		assertArrays(new String[] { "'--format=yyyy-MM-dd HH:mm:ss.SSS'" },
				new String[] { "--format=yyyy-MM-dd HH:mm:ss" + ".SSS" });
		assertArrays(new String[] { "\"--format=yyyy-MM-dd HH:mm:ss.SSS\"" },
				new String[] { "--format=yyyy-MM-dd HH:mm:ss" + ".SSS" });
		assertArrays(new String[] { "--format='yyyy-MM-dd HH:mm:ss.SSS'" },
				new String[] { "--format=yyyy-MM-dd HH:mm:ss" + ".SSS" });
		assertArrays(new String[] { "--format=\"yyyy-MM-dd HH:mm:ss.SSS\"" },
				new String[] { "--format=yyyy-MM-dd HH:mm:ss" + ".SSS" });
		assertArrays(new String[] { "--foo1=bar1 --foo2=bar2" }, new String[] { "--foo1=bar1", "--foo2=bar2" });
		assertArrays(new String[] { "--foo1=bar1", "--foo2=bar2" }, new String[] { "--foo1=bar1", "--foo2=bar2" });
		assertArrays(new String[] { " --foo1=bar1 ", " --foo2=bar2 " }, new String[] { "--foo1=bar1", "--foo2=bar2" });
		assertArrays(new String[] { "'--format=yyyy-MM-dd HH:mm:ss.SSS'", "--foo1=bar1" },
				new String[] { "--format=yyyy-MM-dd HH:mm:ss.SSS", "--foo1=bar1" });
	}
}
