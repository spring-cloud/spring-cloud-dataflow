/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.single.security;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import org.springframework.cloud.dataflow.server.single.LocalDataflowResource;
import org.springframework.data.authentication.UserCredentials;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.dataflow.server.single.security.SecurityTestUtils.basicAuthorizationHeader;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the root URL when security with file-based user list is enabled.
 * Task feature is enabled, while Scheduler, Stream, and Metrics feature are disabled.
 *
 * @author Glenn Renfro
 */
public class LocalServerSecurityRootWithUsersFileTestsWithTasksAndWithoutStreamsMetricsAndSchedules {

	private final static LocalDataflowResource localDataflowResource = new LocalDataflowResource(
			"classpath:org/springframework/cloud/dataflow/server/local/security/fileBasedUsers.yml", false, true, false, false);

	@ClassRule
	public static TestRule springDataflowAndLdapServer = RuleChain.outerRule(localDataflowResource);

	private static UserCredentials viewOnlyUser = new UserCredentials("bob", "bobspassword");
	private static UserCredentials fullUser = new UserCredentials("fulluser", "fullpassword");

	@Test
	@Ignore("Revisit once Outh2/OpenID Connect security is implemented")
	public void testAccessRootUrlAndCheckAllLinksWithFullUser() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/").header("Authorization", basicAuthorizationHeader(fullUser.getUsername(), fullUser.getPassword()))).andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.*", hasSize(23)))
				.andExpect(jsonPath("$._links.dashboard.href", is("http://localhost/dashboard")))
				.andExpect(jsonPath("$._links.streams").doesNotExist())
				.andExpect(jsonPath("$._links.runtime").doesNotExist())
				.andExpect(jsonPath("$._links.metrics").doesNotExist())
				.andExpect(jsonPath("$._links.audit-records.href", is("http://localhost/audit-records")))
				.andExpect(jsonPath("$._links.tasks/definitions.href", is("http://localhost/tasks/definitions")))
				.andExpect(jsonPath("$._links.tasks/definitions/definition.href", is("http://localhost/tasks/definitions/{name}")))
				.andExpect(jsonPath("$._links.tasks/executions.href", is("http://localhost/tasks/executions")))
				.andExpect(jsonPath("$._links.tasks/executions/name.href", is("http://localhost/tasks/executions{?name}")))
				.andExpect(jsonPath("$._links.tasks/executions/current.href", is("http://localhost/tasks/executions/current")))
				.andExpect(jsonPath("$._links.tasks/executions/execution.href", is("http://localhost/tasks/executions/{id}")))
				// TODO com.jayway.jsonpath.InvalidPathException: Illegal character at position 1 expected '.' or '[
				//.andExpect(jsonPath("$_.links.tasks/validation.href", is("http://localhost/tasks/validation/{name}")))
				.andExpect(jsonPath("$._links.jobs/executions.href", is("http://localhost/jobs/executions")))
				.andExpect(jsonPath("$._links.jobs/executions/name.href", is("http://localhost/jobs/executions{?name}")))
				.andExpect(jsonPath("$._links.jobs/executions/execution.href", is("http://localhost/jobs/executions/{id}")))
				.andExpect(jsonPath("$._links.jobs/executions/execution/steps.href", is("http://localhost/jobs/executions/{jobExecutionId}/steps")))
				.andExpect(jsonPath("$._links.jobs/executions/execution/steps/step.href", is("http://localhost/jobs/executions/{jobExecutionId}/steps/{stepId}")))
				.andExpect(jsonPath("$._links.jobs/executions/execution/steps/step/progress.href", is("http://localhost/jobs/executions/{jobExecutionId}/steps/{stepId}/progress")))
				.andExpect(jsonPath("$._links.jobs/instances/name.href", is("http://localhost/jobs/instances{?name}")))
				.andExpect(jsonPath("$._links.jobs/instances/instance.href", is("http://localhost/jobs/instances/{id}")))
				.andExpect(jsonPath("$._links.jobs").doesNotExist())
				.andExpect(jsonPath("$._links.tools/parseTaskTextToGraph.href", is("http://localhost/tools")))
				.andExpect(jsonPath("$._links.tools/convertTaskGraphToText.href", is("http://localhost/tools")))
				.andExpect(jsonPath("$._links.counters").doesNotExist())
				.andExpect(jsonPath("$._links.field-value-counters").doesNotExist())
				.andExpect(jsonPath("$._links.aggregate-counters").doesNotExist())
				.andExpect(jsonPath("$._links.apps.href", is("http://localhost/apps")))
				.andExpect(jsonPath("$._links.about.href", is("http://localhost/about")))
				.andExpect(jsonPath("$._links.completions/stream.href", is("http://localhost/completions/stream{?start,detailLevel}")))
				.andExpect(jsonPath("$._links.completions/task.href", is("http://localhost/completions/task{?start,detailLevel}")));
	}

	@Test
	@Ignore("Revisit once Outh2/OpenID Connect security is implemented")
	public void testAccessRootUrlAndCheckAllLinksWithViewOnlyUser() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/").header("Authorization", basicAuthorizationHeader(viewOnlyUser.getUsername(), viewOnlyUser.getPassword()))).andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.*", hasSize(23)))
				.andExpect(jsonPath("$._links.dashboard.href", is("http://localhost/dashboard")))
				.andExpect(jsonPath("$._links.streams").doesNotExist())
				.andExpect(jsonPath("$._links.runtime").doesNotExist())
				.andExpect(jsonPath("$._links.metrics").doesNotExist())
				.andExpect(jsonPath("$._links.audit-records.href", is("http://localhost/audit-records")))
				.andExpect(jsonPath("$._links.tasks/definitions.href", is("http://localhost/tasks/definitions")))
				.andExpect(jsonPath("$._links.tasks/definitions/definition.href", is("http://localhost/tasks/definitions/{name}")))
				.andExpect(jsonPath("$._links.tasks/executions.href", is("http://localhost/tasks/executions")))
				.andExpect(jsonPath("$._links.tasks/executions/name.href", is("http://localhost/tasks/executions{?name}")))
				.andExpect(jsonPath("$._links.tasks/executions/current.href", is("http://localhost/tasks/executions/current")))
				.andExpect(jsonPath("$._links.tasks/executions/execution.href", is("http://localhost/tasks/executions/{id}")))
				// TODO com.jayway.jsonpath.InvalidPathException: Illegal character at position 1 expected '.' or '[
				//.andExpect(jsonPath("$_.links.tasks/validation.href", is("http://localhost/tasks/validation/{name}")))
				.andExpect(jsonPath("$._links.jobs/executions.href", is("http://localhost/jobs/executions")))
				.andExpect(jsonPath("$._links.jobs/executions/name.href", is("http://localhost/jobs/executions{?name}")))
				.andExpect(jsonPath("$._links.jobs/executions/execution.href", is("http://localhost/jobs/executions/{id}")))
				.andExpect(jsonPath("$._links.jobs/executions/execution/steps.href", is("http://localhost/jobs/executions/{jobExecutionId}/steps")))
				.andExpect(jsonPath("$._links.jobs/executions/execution/steps/step.href", is("http://localhost/jobs/executions/{jobExecutionId}/steps/{stepId}")))
				.andExpect(jsonPath("$._links.jobs/executions/execution/steps/step/progress.href", is("http://localhost/jobs/executions/{jobExecutionId}/steps/{stepId}/progress")))
				.andExpect(jsonPath("$._links.jobs/instances/name.href", is("http://localhost/jobs/instances{?name}")))
				.andExpect(jsonPath("$._links.jobs/instances/instance.href", is("http://localhost/jobs/instances/{id}")))
				.andExpect(jsonPath("$._links.jobs").doesNotExist())
				.andExpect(jsonPath("$._links.tools/parseTaskTextToGraph.href", is("http://localhost/tools")))
				.andExpect(jsonPath("$._links.tools/convertTaskGraphToText.href", is("http://localhost/tools")))
				.andExpect(jsonPath("$._links.counters").doesNotExist())
				.andExpect(jsonPath("$._links.field-value-counters").doesNotExist())
				.andExpect(jsonPath("$._links.aggregate-counters").doesNotExist())
				.andExpect(jsonPath("$._links.apps.href", is("http://localhost/apps")))
				.andExpect(jsonPath("$._links.about.href", is("http://localhost/about")))
				.andExpect(jsonPath("$._links.completions/stream.href", is("http://localhost/completions/stream{?start,detailLevel}")))
				.andExpect(jsonPath("$._links.completions/task.href", is("http://localhost/completions/task{?start,detailLevel}")));
	}
}
