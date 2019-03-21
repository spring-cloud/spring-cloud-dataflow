/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.job.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DeploymentPropertiesUtils}.
 *
 * @author Janne Valkealahti
 */
public class DeploymentPropertiesUtilsTests {

	private static void assertArrays(String[] left, String[] right) {
		ArrayList<String> params = new ArrayList<>(Arrays.asList(left));
		assertThat(DeploymentPropertiesUtils.parseParams(params), containsInAnyOrder(right));
	}

	@Test
	public void testDeploymentPropertiesParsing() {
		Map<String, String> props = DeploymentPropertiesUtils.parse("app.foo.bar=v, app.foo.wizz=v2  , deployer.foo"
				+ ".pot=fern, app.other.key = value  , deployer.other.cow = meww");
		assertThat(props, hasEntry("app.foo.bar", "v"));
		assertThat(props, hasEntry("app.other.key", "value"));
		assertThat(props, hasEntry("app.foo.wizz", "v2"));
		assertThat(props, hasEntry("deployer.foo.pot", "fern"));
		assertThat(props, hasEntry("deployer.other.cow", "meww"));

		props = DeploymentPropertiesUtils.parse("f=v");
		assertThat(props, hasEntry("f", "v"));

		props = DeploymentPropertiesUtils.parse("foo1=bar1,app.foo2=bar2,foo3=bar3,xxx3");
		assertThat(props, hasEntry("foo1", "bar1"));
		assertThat(props, hasEntry("app.foo2", "bar2"));
		assertThat(props, hasEntry("foo3", "bar3,xxx3"));

		props = DeploymentPropertiesUtils.parse("foo1 = bar1 , app.foo2= bar2,  foo3  = bar3,xxx3");
		assertThat(props, hasEntry("foo1", "bar1"));
		assertThat(props, hasEntry("app.foo2", "bar2"));
		assertThat(props, hasEntry("foo3", "bar3,xxx3"));

		props = DeploymentPropertiesUtils.parse("app.*.count=1");
		assertThat(props, hasEntry("app.*.count", "1"));

		props = DeploymentPropertiesUtils.parse("app.*.my-count=1");
		assertThat(props, hasEntry("app.*.my-count", "1"));

		props = DeploymentPropertiesUtils.parse("app.transform.producer.partitionKeyExpression=fakeExpression('xxx')");
		assertThat(props, hasEntry("app.transform.producer.partitionKeyExpression", "fakeExpression('xxx')"));

		props = DeploymentPropertiesUtils.parse("invalidkeyvalue");
		assertThat(props.size(), is(0));

		props = DeploymentPropertiesUtils.parse("invalidkeyvalue1,invalidkeyvalue2");
		assertThat(props.size(), is(0));

		props = DeploymentPropertiesUtils.parse("invalidkeyvalue1,invalidkeyvalue2,foo=bar");
		assertThat(props.size(), is(1));
		assertThat(props, hasEntry("foo", "bar"));

		props = DeploymentPropertiesUtils.parse("invalidkeyvalue1,foo=bar,invalidkeyvalue2");
		assertThat(props.size(), is(1));
		assertThat(props, hasEntry("foo", "bar,invalidkeyvalue2"));

		props = DeploymentPropertiesUtils.parse("foo.bar1=jee1,jee2,jee3,foo.bar2=jee4,jee5,jee6");
		assertThat(props, hasEntry("foo.bar1", "jee1,jee2,jee3"));
		assertThat(props, hasEntry("foo.bar2", "jee4,jee5,jee6"));

		props = DeploymentPropertiesUtils.parse("foo.bar1=xxx=1,foo.bar2=xxx=2");
		assertThat(props, hasEntry("foo.bar1", "xxx=1"));
		assertThat(props, hasEntry("foo.bar2", "xxx=2"));
	}

	@Test
	public void testLongDeploymentPropertyValues() {
		Map<String, String> props = DeploymentPropertiesUtils
				.parse("app.foo.bar=FoooooooooooooooooooooBar,app.foo" + ".bar2=FoooooooooooooooooooooBar");
		assertThat(props, hasEntry("app.foo.bar", "FoooooooooooooooooooooBar"));
		props = DeploymentPropertiesUtils.parse("app.foo.bar=FooooooooooooooooooooooooooooooooooooooooooooooooooooBar");
		assertThat(props, hasEntry("app.foo.bar", "FooooooooooooooooooooooooooooooooooooooooooooooooooooBar"));
	}

	@Test
	public void testDeployerProperties() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("deployer.myapp.count", "2");
		props.put("deployer.myapp.foo", "bar");
		props.put("deployer.otherapp.count", "5");
		props.put("deployer.*.precedence", "wildcard");
		props.put("deployer.myapp.precedence", "app");
		Map<String, String> result = DeploymentPropertiesUtils.extractAndQualifyDeployerProperties(props, "myapp");

		assertThat(result, hasEntry("spring.cloud.deployer.count", "2"));
		assertThat(result, hasEntry("spring.cloud.deployer.foo", "bar"));
		assertThat(result, hasEntry("spring.cloud.deployer.precedence", "app"));
	}

	@Test
	// TO BE REMOVED once deprecated support is removed
	public void testDeprecatedDeployerProperties() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("app.myapp.spring.cloud.deployer.count", "2");
		props.put("app.myapp.spring.cloud.deployer.foo", "bar");
		props.put("app.otherapp.spring.cloud.deployer.count", "5");
		props.put("app.*.spring.cloud.deployer.precedence", "wildcard");
		props.put("app.myapp.spring.cloud.deployer.precedence", "app");
		props.put("app.myapp.something.else", "not-considered-a-deployer-property");
		props.put("deployer.myapp.count", "7");
		props.put("deployer.myapp.foo", "wizz");
		props.put("deployer.otherapp.count", "6");
		props.put("deployer.*.precedence", "wildcard");
		props.put("deployer.myapp.precedence", "the-app");

		Map<String, String> result = DeploymentPropertiesUtils.extractAndQualifyDeployerProperties(props, "myapp");
		assertThat(result.keySet(), hasSize(3));
		assertThat(result, hasEntry("spring.cloud.deployer.count", "7"));
		assertThat(result, hasEntry("spring.cloud.deployer.foo", "wizz"));
		assertThat(result, hasEntry("spring.cloud.deployer.precedence", "the-app"));
	}

	@Test
	// TO BE REMOVED once deprecated support is removed
	public void testDeprecatedDeployerPropertiesMixed() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("app.myapp.spring.cloud.deployer.count", "2");
		props.put("app.myapp.spring.cloud.deployer.foo", "bar");
		props.put("app.otherapp.spring.cloud.deployer.count", "5");
		props.put("app.*.spring.cloud.deployer.precedence", "wildcard");
		props.put("app.myapp.spring.cloud.deployer.precedence", "app");
		props.put("app.myapp.something.else", "not-considered-a-deployer-property");

		Map<String, String> result = DeploymentPropertiesUtils.extractAndQualifyDeployerProperties(props, "myapp");
		assertThat(result.keySet(), hasSize(3));
		assertThat(result, hasEntry("spring.cloud.deployer.count", "2"));
		assertThat(result, hasEntry("spring.cloud.deployer.foo", "bar"));
		assertThat(result, hasEntry("spring.cloud.deployer.precedence", "app"));
	}

	@Test
	// TO BE REMOVED once deprecated support is removed
	public void testDeprecatedDeployerPropertyCount() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("app.myapp.count", "2");
		Map<String, String> result = DeploymentPropertiesUtils.extractAndQualifyDeployerProperties(props, "myapp");
		assertThat(result, hasEntry("spring.cloud.deployer.count", "2"));
	}

	@Test
	public void testCommandLineParamsParsing() {
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
