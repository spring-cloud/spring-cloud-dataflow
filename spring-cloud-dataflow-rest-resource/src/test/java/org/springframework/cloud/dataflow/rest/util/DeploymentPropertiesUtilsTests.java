/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import org.springframework.util.FileCopyUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link DeploymentPropertiesUtils}.
 *
 * @author Janne Valkealahti
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
public class DeploymentPropertiesUtilsTests {

	private static void assertArrays(String[] left, String[] right) {
		ArrayList<String> params = new ArrayList<>(Arrays.asList(left));
		MatcherAssert.assertThat(DeploymentPropertiesUtils.removeQuoting(params), containsInAnyOrder(right));
	}

	@Test
	public void testDeploymentPropertiesParsing() {
		Map<String, String> props = DeploymentPropertiesUtils.parse("app.foo.bar=v, app.foo.wizz=v2  , deployer.foo"
				+ ".pot=fern, app.other.key = value  , deployer.other.cow = meww, scheduler.other.key = baz");
		MatcherAssert.assertThat(props, hasEntry("app.foo.bar", "v"));
		MatcherAssert.assertThat(props, hasEntry("app.other.key", "value"));
		MatcherAssert.assertThat(props, hasEntry("app.foo.wizz", "v2"));
		MatcherAssert.assertThat(props, hasEntry("deployer.foo.pot", "fern"));
		MatcherAssert.assertThat(props, hasEntry("deployer.other.cow", "meww"));
		MatcherAssert.assertThat(props, hasEntry("scheduler.other.key", "baz"));

		props = DeploymentPropertiesUtils.parse("app.f=v");
		MatcherAssert.assertThat(props, hasEntry("app.f", "v"));

		props = DeploymentPropertiesUtils.parse("app.foo1=bar1,app.foo2=bar2,app.foo3=bar3,xxx3");
		MatcherAssert.assertThat(props, hasEntry("app.foo1", "bar1"));
		MatcherAssert.assertThat(props, hasEntry("app.foo2", "bar2"));
		MatcherAssert.assertThat(props, hasEntry("app.foo3", "bar3,xxx3"));

		props = DeploymentPropertiesUtils.parse("deployer.foo1 = bar1 , app.foo2= bar2,  deployer.foo3  = bar3,xxx3");
		MatcherAssert.assertThat(props, hasEntry("deployer.foo1", "bar1"));
		MatcherAssert.assertThat(props, hasEntry("app.foo2", "bar2"));
		MatcherAssert.assertThat(props, hasEntry("deployer.foo3", "bar3,xxx3"));

		props = DeploymentPropertiesUtils.parse("app.*.count=1");
		MatcherAssert.assertThat(props, hasEntry("app.*.count", "1"));

		props = DeploymentPropertiesUtils.parse("app.*.my-count=1");
		MatcherAssert.assertThat(props, hasEntry("app.*.my-count", "1"));

		props = DeploymentPropertiesUtils.parse("app.transform.producer.partitionKeyExpression=fakeExpression('xxx')");
		MatcherAssert.assertThat(props, hasEntry("app.transform.producer.partitionKeyExpression", "fakeExpression('xxx')"));

		try {
			DeploymentPropertiesUtils.parse("invalidkeyvalue");
			fail("Illegal Argument Exception expected.");
		}
		catch (Exception e) {
			assertTrue(e.getMessage().equals("Only deployment property keys starting with 'app.' or 'scheduler' or 'deployer.'  or 'version.' allowed."));
		}

		props = DeploymentPropertiesUtils.parse("deployer.foo=bar,invalidkeyvalue2");
		MatcherAssert.assertThat(props.size(), is(1));
		MatcherAssert.assertThat(props, hasEntry("deployer.foo", "bar,invalidkeyvalue2"));

		props = DeploymentPropertiesUtils.parse("app.foo.bar1=jee1,jee2,jee3,deployer.foo.bar2=jee4,jee5,jee6");
		MatcherAssert.assertThat(props, hasEntry("app.foo.bar1", "jee1,jee2,jee3"));
		MatcherAssert.assertThat(props, hasEntry("deployer.foo.bar2", "jee4,jee5,jee6"));

		props = DeploymentPropertiesUtils.parse("app.foo.bar1=xxx=1,app.foo.bar2=xxx=2");
		MatcherAssert.assertThat(props, hasEntry("app.foo.bar1", "xxx=1"));
		MatcherAssert.assertThat(props, hasEntry("app.foo.bar2", "xxx=2"));

		props = DeploymentPropertiesUtils.parse("app.foo.bar1=xxx=1,test=value,app.foo.bar2=xxx=2");
		MatcherAssert.assertThat(props, hasEntry("app.foo.bar1", "xxx=1,test=value"));
		MatcherAssert.assertThat(props, hasEntry("app.foo.bar2", "xxx=2"));
	}


	@Test
	public void testDeploymentPropertiesParsing2() {
		List<String> props = DeploymentPropertiesUtils.parseParamList("app.foo.bar=v, app.foo.wizz=v2  , deployer.foo"
				+ ".pot=fern, app.other.key = value  , deployer.other.cow = meww,special=koza=boza,more", ",");

		assertTrue(props.contains("app.foo.bar=v"));
		assertTrue(props.contains(" app.other.key = value  "));
		assertTrue(props.contains(" app.foo.wizz=v2  "));
		assertTrue(props.contains(" deployer.foo.pot=fern"));
		assertTrue(props.contains(" deployer.other.cow = meww,special=koza=boza,more"));

		try {
			DeploymentPropertiesUtils.parseParamList("a=b", " ");
			fail("Illegal Argument Exception expected.");
		}
		catch (Exception e) {
			assertTrue(e.getMessage().equals("Only deployment property keys starting with 'app.' or 'scheduler' or 'deployer.'  or 'version.' allowed."));
		}

		props = DeploymentPropertiesUtils.parseArgumentList("a=b c=d", " ");
		assertTrue(props.contains("c=d"));
		assertTrue(props.contains("a=b"));

		props = DeploymentPropertiesUtils.parseArgumentList("foo1=bar1 foo2=bar2 foo3=bar3 xxx3", " ");
		assertTrue(props.contains("foo1=bar1"));
		assertTrue(props.contains("foo2=bar2"));
		assertTrue(props.contains("foo3=bar3 xxx3"));
	}

	@Test
	public void parseArgumentTestsWithQuotes() {

		List<String> props = DeploymentPropertiesUtils.parseArgumentList("a=\"b c\" e=f g=h", " ");
		assertTrue(props.contains("a=\"b c\""));
		assertTrue(props.contains("e=f"));
		assertTrue(props.contains("g=h"));
		props = DeploymentPropertiesUtils.parseArgumentList("--composedTaskArguments=\"1.timestamp.format=YYYY " +
				"--timestamp.timestamp.format=MM --foo=bar bar=\"bazzz buzz\"\" " +
				"a=b c=d --foo=bar", " ");
		assertTrue(props.contains("--composedTaskArguments=\"1.timestamp.format=YYYY " +
				"--timestamp.timestamp.format=MM --foo=bar bar=\"bazzz buzz\"\""));
		assertTrue(props.contains("a=b"));
		assertTrue(props.contains("c=d"));
		assertTrue(props.contains("--foo=bar"));
	}

	@Test
	public void testLongDeploymentPropertyValues() {
		Map<String, String> props = DeploymentPropertiesUtils
				.parse("app.foo.bar=FoooooooooooooooooooooBar,app.foo" + ".bar2=FoooooooooooooooooooooBar");
		MatcherAssert.assertThat(props, hasEntry("app.foo.bar", "FoooooooooooooooooooooBar"));
		props = DeploymentPropertiesUtils.parse("app.foo.bar=FooooooooooooooooooooooooooooooooooooooooooooooooooooBar");
		MatcherAssert.assertThat(props, hasEntry("app.foo.bar", "FooooooooooooooooooooooooooooooooooooooooooooooooooooBar"));
	}

	@Test
	public void testDeployerProperties() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("app.myapp.foo", "bar");
		props.put("deployer.myapp.count", "2");
		props.put("deployer.myapp.foo", "bar");
		props.put("deployer.otherapp.count", "5");
		props.put("deployer.*.precedence", "wildcard");
		props.put("deployer.myapp.precedence", "app");
		Map<String, String> result = DeploymentPropertiesUtils.extractAndQualifyDeployerProperties(props, "myapp");

		MatcherAssert.assertThat(result, hasEntry("spring.cloud.deployer.count", "2"));
		MatcherAssert.assertThat(result, hasEntry("spring.cloud.deployer.foo", "bar"));
		MatcherAssert.assertThat(result, hasEntry("spring.cloud.deployer.precedence", "app"));
		MatcherAssert.assertThat(result, not(hasKey("app.myapp.foo")));
	}

	@Test
	public void testDeployerPropertiesWithApp() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("app.myapp.foo", "bar");
		props.put("deployer.myapp.count", "2");
		props.put("deployer.myapp.foo", "bar");
		props.put("deployer.otherapp.count", "5");
		props.put("deployer.*.precedence", "wildcard");
		props.put("deployer.myapp.precedence", "app");
		Map<String, String> result = DeploymentPropertiesUtils.qualifyDeployerProperties(props, "myapp");

		MatcherAssert.assertThat(result, hasEntry("spring.cloud.deployer.count", "2"));
		MatcherAssert.assertThat(result, hasEntry("spring.cloud.deployer.foo", "bar"));
		MatcherAssert.assertThat(result, hasEntry("spring.cloud.deployer.precedence", "app"));
		MatcherAssert.assertThat(result, hasKey("app.myapp.foo"));
	}

	@Test
	public void testCommandLineParamsParsing() {
		assertArrays(new String[] { "--format=x,y,z" }, new String[] { "--format=x,y,z" });
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

	@Test
	public void testParseDeploymentProperties() throws IOException {
		File file = Files.createTempFile(null, ".yaml").toFile();
		FileCopyUtils.copy("app.foo1:\n  bar1: spam".getBytes(), file);

		Map<String, String> props = DeploymentPropertiesUtils.parseDeploymentProperties("app.foo2=bar2", file, 0);
		MatcherAssert.assertThat(props.size(), is(1));
		MatcherAssert.assertThat(props.get("app.foo2"), is("bar2"));

		props = DeploymentPropertiesUtils.parseDeploymentProperties("foo2=bar2", file, 1);
		MatcherAssert.assertThat(props.size(), is(1));
		MatcherAssert.assertThat(props.get("app.foo1.bar1"), is("spam"));
	}
}
