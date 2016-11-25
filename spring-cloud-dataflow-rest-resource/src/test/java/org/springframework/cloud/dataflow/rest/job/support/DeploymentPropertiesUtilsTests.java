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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
	public void testDeploymentPropertiesParsing() {
		Map<String, String> props = DeploymentPropertiesUtils.parse("app.foo.bar=v, app.foo.wizz=v2  , deployer.foo.pot=fern, app.other.key = value  , deployer.other.cow = meww");
		System.out.println(props);
		assertThat(props, hasEntry("app.foo.bar", "v"));
		assertThat(props, hasEntry("app.other.key", "value"));
		assertThat(props, hasEntry("app.foo.wizz", "v2"));
		assertThat(props, hasEntry("deployer.foo.pot", "fern"));
		assertThat(props, hasEntry("deployer.other.cow", "meww"));
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

	private static void assertArrays(String[] left, String[] right) {
		ArrayList<String> params = new ArrayList<>(Arrays.asList(left));
		assertThat(DeploymentPropertiesUtils.parseParams(params), containsInAnyOrder(right));
	}
}
