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
package org.springframework.cloud.dataflow.server.local.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.local.LocalDataflowResource;
import org.springframework.cloud.dataflow.server.local.TestUtils;
import org.springframework.data.authentication.UserCredentials;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.CollectionUtils;

import static org.springframework.cloud.dataflow.server.local.security.SecurityTestUtils.basicAuthorizationHeader;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for security configuration backed by a file-based user list. This test-suite is
 * basically the same as {@link LocalServerSecurityWithUsersFileTests} but with the Task
 * feature disabled. Therefore any REST endpoints related to Jobs and Tasks should return
 * a 404 (Not Found) HTTP status code.
 *
 * @author Gunnar Hillert
 */
@RunWith(Parameterized.class)
public class LocalServerSecurityWithUsersFileTestsWithoutTasks {

	private final static Logger logger = LoggerFactory
			.getLogger(LocalServerSecurityWithUsersFileTestsWithoutTasks.class);

	private final static LocalDataflowResource localDataflowResource = new LocalDataflowResource(
			"classpath:org/springframework/cloud/dataflow/server/local/security" + "/fileBasedUsers.yml", true, false);

	@ClassRule
	public static TestRule springDataflowAndLdapServer = RuleChain.outerRule(localDataflowResource);

	private static UserCredentials viewOnlyUser = new UserCredentials("bob", "bobspassword");

	private static UserCredentials adminOnlyUser = new UserCredentials("alice", "alicepwd");

	private static UserCredentials createOnlyUser = new UserCredentials("cartman", "cartmanpwd");

	@Parameter(value = 0)
	public HttpMethod httpMethod;

	@Parameter(value = 1)
	public HttpStatus expectedHttpStatus;

	@Parameter(value = 2)
	public String url;

	@Parameter(value = 3)
	public UserCredentials userCredentials;

	@Parameter(value = 4)
	public Map<String, String> urlParameters;

	@Parameters(name = "Authentication Test {index} - {0} {2} - Returns: {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {

				{ HttpMethod.GET, HttpStatus.OK, "/", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/", null, null },

				/* AppRegistryController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/apps", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/apps", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/apps", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/apps", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/apps/task/taskname", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/apps/task/taskname", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/apps/task/taskname", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/apps/task/taskname", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/apps/task/taskname", adminOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/apps/task/taskname", viewOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/apps/task/taskname", createOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.CREATED, "/apps/task/taskname", createOnlyUser,
						TestUtils.toImmutableMap("uri", "maven://io.spring.cloud:scdf-sample-app:jar:1.0.0.BUILD-SNAPSHOT",
								"force", "false") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/apps/task/taskname", null, null },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/apps/task/taskname", adminOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/apps/task/taskname", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.OK, "/apps/task/taskname", createOnlyUser, null }, // Should
																									// be
																									// 404
																									// -
				// See https://github.com/spring-cloud/spring-cloud-dataflow/issues/1071
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/apps/task/taskname", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/apps", adminOnlyUser,
						TestUtils.toImmutableMap("uri", "???", "apps", "??", "force", "true") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/apps", viewOnlyUser,
						TestUtils.toImmutableMap("uri", "???", "apps", "??", "force", "true") },
				{ HttpMethod.POST, HttpStatus.CREATED, "/apps", createOnlyUser,
						TestUtils.toImmutableMap("uri", "http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/spring-cloud-stream-app-descriptor/Celsius.SR1/spring-cloud-stream-app-descriptor-Celsius.SR1.stream-apps-rabbit-maven", "apps",
								"app=is_ignored", "force", "false") },
				// Should be 400 -
				// See https://github.com/spring-cloud/spring-cloud-dataflow/issues/1071
				{ HttpMethod.POST, HttpStatus.CREATED, "/apps", createOnlyUser,
						TestUtils.toImmutableMap("uri", "http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/spring-cloud-stream-app-descriptor/Celsius.SR1/spring-cloud-stream-app-descriptor-Celsius.SR1.stream-apps-rabbit-maven", "force",
								"false") },
				{ HttpMethod.POST, HttpStatus.INTERNAL_SERVER_ERROR, "/apps", createOnlyUser,
						TestUtils.toImmutableMap("apps",
								"appTypeMissing=maven://io.spring.cloud:scdf-sample-app:jar:1.0.0.BUILD-SNAPSHOT",
								"force", "false") }, // Should be 400 - See https://github
				// .com/spring-cloud/spring-cloud-dataflow/issues/1071
				{ HttpMethod.POST, HttpStatus.CREATED, "/apps", createOnlyUser,
						TestUtils.toImmutableMap("apps",
								"task" + ".myCoolApp=maven://io.spring.cloud:scdf-sample-app:jar:1.0.0.BUILD-SNAPSHOT",
								"force", "false") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/apps", null,
						TestUtils.toImmutableMap("uri", "???", "apps", "??", "force", "true") },

				/* CompletionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/stream", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/stream", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/completions/stream", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/completions/stream", createOnlyUser, TestUtils.toImmutableMap("start", "2") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/completions/stream", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/task", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/task", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/completions/task", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/completions/task", createOnlyUser, TestUtils.toImmutableMap("start", "2") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/completions/task", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/stream", adminOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/stream", viewOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.OK, "/completions/stream", createOnlyUser,
						TestUtils.toImmutableMap("start", "2", "detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/completions/stream", createOnlyUser,
						TestUtils.toImmutableMap("start", "2", "detailLevel", "-123") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/completions/stream", null,
						TestUtils.toImmutableMap("detailLevel", "2") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/task", adminOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/task", viewOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.OK, "/completions/task", createOnlyUser,
						TestUtils.toImmutableMap("start", "2", "detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/completions/task", createOnlyUser,
						TestUtils.toImmutableMap("start", "2", "detailLevel", "-123") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/completions/task", null,
						TestUtils.toImmutableMap("detailLevel", "2") },

				/* JobExecutionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", adminOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions", viewOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", createOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions", null,
						TestUtils.toImmutableMap("page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", adminOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions", null,
						TestUtils.toImmutableMap("name", "myname") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", adminOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions", null,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123", null, null },

				{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions/123", adminOnlyUser,
						TestUtils.toImmutableMap("stop", "true") },
				{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions/123", viewOnlyUser,
						TestUtils.toImmutableMap("stop", "true") },
				{ HttpMethod.PUT, HttpStatus.NOT_FOUND, "/jobs/executions/123", createOnlyUser,
						TestUtils.toImmutableMap("stop", "true") },
				{ HttpMethod.PUT, HttpStatus.UNAUTHORIZED, "/jobs/executions/123", null,
						TestUtils.toImmutableMap("stop", "true") },

				{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions/123", adminOnlyUser,
						TestUtils.toImmutableMap("restart", "true") },
				{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions/123", viewOnlyUser,
						TestUtils.toImmutableMap("restart", "true") },
				{ HttpMethod.PUT, HttpStatus.NOT_FOUND, "/jobs/executions/123", createOnlyUser,
						TestUtils.toImmutableMap("restart", "true") },
				{ HttpMethod.PUT, HttpStatus.UNAUTHORIZED, "/jobs/executions/123", null,
						TestUtils.toImmutableMap("restart", "true") },

				/* JobInstanceController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", adminOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/instances", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/instances", null,
						TestUtils.toImmutableMap("name", "my-job-name") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", adminOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/instances", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/instances", null,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/instances", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/instances", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances/123", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/instances/123", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances/123", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/instances/123", null, null },

				/* JobStepExecutionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123/steps", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123/steps", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/abc/steps", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/abc/steps", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/abc/steps", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/abc/steps", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps", adminOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123/steps", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123/steps", null,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps/1", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123/steps/1", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps/1", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123/steps/1", null, null },

				/* JobStepExecutionProgressController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps/1/progress", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123/steps/1/progress", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps/1/progress", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123/steps/1/progress", null, null },

				/* RuntimeAppsController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/runtime/apps", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/runtime/apps", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/runtime/apps/123", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/runtime/apps/123", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123/instances", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/runtime/apps/123/instances", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123/instances", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/runtime/apps/123/instances", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123/instances/456", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/runtime/apps/123/instances/456", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123/instances/456", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/runtime/apps/123/instances/456", null, null },

				/* StreamDefinitionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/streams/definitions", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", adminOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.OK, "/streams/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", createOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions", null,
						TestUtils.toImmutableMap("page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", adminOnlyUser,
						TestUtils.toImmutableMap("search", "mysearch") },
				{ HttpMethod.GET, HttpStatus.OK, "/streams/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("search", "mysearch") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", createOnlyUser,
						TestUtils.toImmutableMap("search", "mysearch") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions", null,
						TestUtils.toImmutableMap("search", "mysearch") },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/definitions", adminOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "definition", "fooo | baaar") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "definition", "fooo | baaar") },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/streams/definitions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "definition", "fooo | baaar") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/streams/definitions", null,
						TestUtils.toImmutableMap("name", "myname", "definition", "fooo | baaar") },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/definitions", adminOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/streams/definitions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/streams/definitions", null,
						TestUtils.toImmutableMap("name", "myname") },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions/delete-me", adminOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions/delete-me", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/streams/definitions/delete-me", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/streams/definitions/delete-me", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream/related", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/streams/definitions/my-stream/related", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream/related", createOnlyUser,
						null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions/my-stream/related", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream/related", adminOnlyUser,
						TestUtils.toImmutableMap("nested", "wrong-param") },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/streams/definitions/my-stream/related", viewOnlyUser,
						TestUtils.toImmutableMap("nested", "wrong-param") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream/related", createOnlyUser,
						TestUtils.toImmutableMap("nested", "wrong-param") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions/my-stream/related", null,
						TestUtils.toImmutableMap("nested", "wrong-param") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/streams/definitions/my-stream", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions/my-stream", null, null },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions", adminOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.OK, "/streams/definitions", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/streams/definitions", null, null },

				/* StreamDeploymentController */

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments", adminOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.OK, "/streams/deployments", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/streams/deployments", null, null },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments/my-stream", adminOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments/my-stream", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/streams/deployments/my-stream", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/streams/deployments/my-stream", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/deployments/my-stream", adminOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/deployments/my-stream", viewOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/streams/deployments/my-stream", createOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/streams/deployments/my-stream", null, null },

				/* TaskDefinitionController */

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/definitions", adminOnlyUser,
						TestUtils.toImmutableMap("name", "my-name") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-name") },
				{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/tasks/definitions", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-name") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/tasks/definitions", null,
						TestUtils.toImmutableMap("name", "my-name") },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/definitions", adminOnlyUser,
						TestUtils.toImmutableMap("name", "my-name", "definition", "foo") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-name", "definition", "foo") },
				{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/tasks/definitions", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-name", "definition", "foo") }, // Should
																					// be
																					// a
																					// `400`
																					// error
																					// -
																					// See
																					// also:
																					// https://github
				// .com/spring-cloud/spring-cloud-dataflow/issues/1075
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/tasks/definitions", null,
						TestUtils.toImmutableMap("name", "my-name", "definition", "foo") },

				/* TaskExecutionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/tasks/executions", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tasks/executions", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", adminOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/tasks/executions", viewOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", createOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tasks/executions", null,
						TestUtils.toImmutableMap("page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", adminOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/tasks/executions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tasks/executions", null,
						TestUtils.toImmutableMap("name", "my-task-name") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions/123", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/tasks/executions/123", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions/123", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tasks/executions/123", null, null },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/tasks/executions/123", adminOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/tasks/executions/123", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/tasks/executions/123", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/tasks/executions/123", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions", adminOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions", viewOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/tasks/executions", createOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/tasks/executions", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions", adminOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/tasks/executions", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/tasks/executions", null,
						TestUtils.toImmutableMap("name", "my-task-name") },

				/* UiController */

				{ HttpMethod.GET, HttpStatus.FOUND, "/dashboard", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FOUND, "/dashboard", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FOUND, "/dashboard", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FOUND, "/dashboard", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/about", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/about", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/about", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/about", null, null },

				{ HttpMethod.GET, HttpStatus.OK, "/management", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/management", null, null },

				{ HttpMethod.GET, HttpStatus.OK, "/management/info", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management/info", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management/info", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/management/info", null, null },

				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/management/does-not-exist", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management/does-not-exist", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management/does-not-exist", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/management/does-not-exist", null, null },

				// Requires Redis
				// { HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/counters",
				// adminOnlyUser, null },
				// { HttpMethod.GET, HttpStatus.OK, "/metrics/counters", viewOnlyUser,
				// null },
				// { HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/counters",
				// createOnlyUser, null },
				// { HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/metrics/counters", null,
				// null },

				/* LoginController */

				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/authenticate", adminOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/authenticate", viewOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/authenticate", createOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/authenticate", null, null },

				/* SecurityController */

				{ HttpMethod.GET, HttpStatus.OK, "/security/info", adminOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/security/info", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/security/info", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/security/info", null, null } });
	}

	@Test
	public void testEndpointAuthentication() throws Exception {

		logger.info(String.format(
				"Using parameters - httpMethod: %s, " + "URL: %s, URL parameters: %s, user credentials: %s",
				this.httpMethod, this.url, this.urlParameters, userCredentials));

		final MockHttpServletRequestBuilder rb;

		switch (httpMethod) {
		case GET:
			rb = get(url);
			break;
		case POST:
			rb = post(url);
			break;
		case PUT:
			rb = put(url);
			break;
		case DELETE:
			rb = delete(url);
			break;
		default:
			throw new IllegalArgumentException("Unsupported Method: " + httpMethod);
		}

		if (this.userCredentials != null) {
			rb.header("Authorization",
					basicAuthorizationHeader(this.userCredentials.getUsername(), this.userCredentials.getPassword()));
		}

		if (!CollectionUtils.isEmpty(urlParameters)) {
			for (Map.Entry<String, String> mapEntry : urlParameters.entrySet()) {
				rb.param(mapEntry.getKey(), mapEntry.getValue());
			}
		}

		final ResultMatcher statusResultMatcher;

		switch (expectedHttpStatus) {
		case UNAUTHORIZED:
			statusResultMatcher = status().isUnauthorized();
			break;
		case FORBIDDEN:
			statusResultMatcher = status().isForbidden();
			break;
		case FOUND:
			statusResultMatcher = status().isFound();
			break;
		case NOT_FOUND:
			statusResultMatcher = status().isNotFound();
			break;
		case OK:
			statusResultMatcher = status().isOk();
			break;
		case CREATED:
			statusResultMatcher = status().isCreated();
			break;
		case BAD_REQUEST:
			statusResultMatcher = status().isBadRequest();
			break;
		case INTERNAL_SERVER_ERROR:
			statusResultMatcher = status().isInternalServerError();
			break;
		default:
			throw new IllegalArgumentException("Unsupported Status: " + expectedHttpStatus);
		}

		try {
			localDataflowResource.getMockMvc().perform(rb).andDo(print()).andExpect(statusResultMatcher);
		}
		catch (AssertionError e) {
			throw new AssertionError(String.format(
					"Assertion failed for parameters - httpMethod: %s, "
							+ "URL: %s, URL parameters: %s, user credentials: %s",
					this.httpMethod, this.url, this.urlParameters, this.userCredentials), e);
		}
	}
}
