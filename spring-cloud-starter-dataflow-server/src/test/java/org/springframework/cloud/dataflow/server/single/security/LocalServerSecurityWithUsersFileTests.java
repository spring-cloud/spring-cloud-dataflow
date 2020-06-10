/*
 * Copyright 2016-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.single.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
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

import org.springframework.cloud.dataflow.server.single.LocalDataflowResource;
import org.springframework.cloud.dataflow.server.single.TestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.CollectionUtils;

import static org.springframework.cloud.dataflow.server.single.security.SecurityTestUtils.basicAuthorizationHeader;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for security configuration backed by a file-based user list.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@RunWith(Parameterized.class)
public class LocalServerSecurityWithUsersFileTests {

	private final static Logger logger = LoggerFactory.getLogger(LocalServerSecurityWithUsersFileTests.class);

	private final static OAuth2ServerResource oAuth2ServerResource = new OAuth2ServerResource();

	private final static LocalDataflowResource localDataflowResource = new LocalDataflowResource(
			"classpath:org/springframework/cloud/dataflow/server/single/security/oauthConfig.yml");

	@ClassRule
	public static TestRule springDataflowAndOAuth2Server = RuleChain.outerRule(oAuth2ServerResource)
			.around(localDataflowResource);

	private static UserCredentials viewOnlyUser = new UserCredentials("bob", "bobspassword");

	private static UserCredentials manageOnlyUser = new UserCredentials("alice", "alicepwd");

	private static UserCredentials createOnlyUser = new UserCredentials("cartman", "cartmanpwd");

	@Parameter(0)
	public HttpMethod httpMethod;

	@Parameter(1)
	public HttpStatus expectedHttpStatus;

	@Parameter(2)
	public String url;

	@Parameter(3)
	public UserCredentials userCredentials;

	@Parameter(4)
	public Map<String, String> urlParameters;

	@Before
	public void before() {
		localDataflowResource.mockSkipperAboutInfo();
	}

	@Parameters(name = "Authentication Test {index} - {0} {2} - Returns: {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {

				{ HttpMethod.GET, HttpStatus.OK, "/", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/", null, null },

				/* AppRegistryController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/apps", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/apps", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/apps", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/apps", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/apps/task/taskname", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/apps/task/taskname", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/apps/task/taskname", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/apps/task/taskname", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/apps/task/taskname", manageOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/apps/task/taskname", viewOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/apps/task/taskname", createOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.CREATED, "/apps/task/taskname", createOnlyUser,
						TestUtils.toImmutableMap("uri", "maven://io.spring.cloud:scdf-sample-app:jar:1.0.0.BUILD-SNAPSHOT",
								"force", "false") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/apps/task/taskname", null, null },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/apps/task/taskname", manageOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/apps/task/taskname", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.OK, "/apps/task/taskname", createOnlyUser, null }, // Should be 404 - See https://github.com/spring-cloud/spring-cloud-dataflow/issues/1071
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/apps/task/taskname", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/apps", manageOnlyUser,
						TestUtils.toImmutableMap("uri", "???", "apps", "??", "force", "true") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/apps", viewOnlyUser,
						TestUtils.toImmutableMap("uri", "???", "apps", "??", "force", "true") },
				{ HttpMethod.POST, HttpStatus.CREATED, "/apps", createOnlyUser,
						TestUtils.toImmutableMap("uri", "https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/spring-cloud-stream-app-descriptor/Celsius.SR1/spring-cloud-stream-app-descriptor-Celsius.SR1.stream-apps-rabbit-maven", "apps",
								"app=is_ignored", "force", "false") },
				// Should be 400 -
				// See https://github.com/spring-cloud/spring-cloud-dataflow/issues/1071
				{ HttpMethod.POST, HttpStatus.CREATED, "/apps", createOnlyUser,
						TestUtils.toImmutableMap("uri", "https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/spring-cloud-stream-app-descriptor/Celsius.SR1/spring-cloud-stream-app-descriptor-Celsius.SR1.stream-apps-rabbit-maven", "force",
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

				/* AuditController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/audit-records", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/audit-records", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/audit-records", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/audit-records", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/audit-records/1", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/audit-records/1", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/audit-records/1", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/audit-records/1", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/audit-records/audit-action-types", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/audit-records/audit-action-types", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/audit-records/audit-action-types", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/audit-records/audit-action-types", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/audit-records/audit-operation-types", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/audit-records/audit-operation-types", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/audit-records/audit-operation-types", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/audit-records/audit-operation-types", null, null },

				/* CompletionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/stream", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/stream", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/completions/stream", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/completions/stream", viewOnlyUser, TestUtils.toImmutableMap("start", "2") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/completions/stream", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/task", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/task", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/completions/task", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/completions/task", viewOnlyUser, TestUtils.toImmutableMap("start", "2") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/completions/task", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/stream", manageOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2","start", "0") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/stream", createOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2","start", "0") },
				{ HttpMethod.GET, HttpStatus.OK, "/completions/stream", viewOnlyUser,
						TestUtils.toImmutableMap("start", "2", "detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/completions/stream", viewOnlyUser,
						TestUtils.toImmutableMap("start", "2", "detailLevel", "-123") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/completions/stream", null,
						TestUtils.toImmutableMap("detailLevel", "2") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/task", manageOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/completions/task", createOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.OK, "/completions/task", viewOnlyUser,
						TestUtils.toImmutableMap("start", "2", "detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/completions/task", viewOnlyUser,
						TestUtils.toImmutableMap("start", "2", "detailLevel", "-123") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/completions/task", null,
						TestUtils.toImmutableMap("detailLevel", "2") },

				/* ToolsController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tools/parseTaskTextToGraph", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tools/parseTaskTextToGraph", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tools/parseTaskTextToGraph", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tools/convertTaskGraphToText", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tools/convertTaskGraphToText", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tools/convertTaskGraphToText", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tools/parseTaskTextToGraph", manageOnlyUser,
						TestUtils.toImmutableMap("definition", "fooApp") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tools/parseTaskTextToGraph", createOnlyUser,
						TestUtils.toImmutableMap("definition", "fooApp") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tools/parseTaskTextToGraph", null,
						TestUtils.toImmutableMap("definition", "fooApp") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tools/convertTaskGraphToText", manageOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tools/convertTaskGraphToText", createOnlyUser,
						TestUtils.toImmutableMap("detailLevel", "2") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tools/convertTaskGraphToText", null,
						TestUtils.toImmutableMap("detailLevel", "2") },

				/* JobExecutionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/jobs/executions", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", manageOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.OK, "/jobs/executions", viewOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", createOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions", null,
						TestUtils.toImmutableMap("page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions", null,
						TestUtils.toImmutableMap("name", "myname") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions", null,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123", null, null },

				{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions/123", manageOnlyUser,
						TestUtils.toImmutableMap("stop", "true") },
				{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions/123", viewOnlyUser,
						TestUtils.toImmutableMap("stop", "true") },
				{ HttpMethod.PUT, HttpStatus.NOT_FOUND, "/jobs/executions/123", createOnlyUser,
						TestUtils.toImmutableMap("stop", "true") },
				{ HttpMethod.PUT, HttpStatus.UNAUTHORIZED, "/jobs/executions/123", null,
						TestUtils.toImmutableMap("stop", "true") },

				{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions/123", manageOnlyUser,
						TestUtils.toImmutableMap("restart", "true") },
				{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions/123", viewOnlyUser,
						TestUtils.toImmutableMap("restart", "true") },
				{ HttpMethod.PUT, HttpStatus.NOT_FOUND, "/jobs/executions/123", createOnlyUser,
						TestUtils.toImmutableMap("restart", "true") },
				{ HttpMethod.PUT, HttpStatus.UNAUTHORIZED, "/jobs/executions/123", null,
						TestUtils.toImmutableMap("restart", "true") },

				/* JobExecutionThinController */


				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/thinexecutions", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/jobs/thinexecutions", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/thinexecutions", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/thinexecutions", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/thinexecutions", manageOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.OK, "/jobs/thinexecutions", viewOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/thinexecutions", createOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/thinexecutions", null,
						TestUtils.toImmutableMap("page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/thinexecutions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/thinexecutions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/thinexecutions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/thinexecutions", null,
						TestUtils.toImmutableMap("name", "myname") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/thinexecutions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/thinexecutions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/thinexecutions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/thinexecutions", null,
						TestUtils.toImmutableMap("name", "myname", "page", "0", "size", "10") },

				/* JobInstanceController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", manageOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/instances", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/instances", null,
						TestUtils.toImmutableMap("name", "my-job-name") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", manageOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/instances", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/instances", null,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/jobs/instances", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/instances", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances/123", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/instances/123", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances/123", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/instances/123", null, null },

				/* JobStepExecutionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123/steps", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123/steps", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/abc/steps", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/jobs/executions/abc/steps", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/abc/steps", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/abc/steps", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps", manageOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123/steps", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123/steps", null,
						TestUtils.toImmutableMap("name", "my-job-name", "page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps/1", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123/steps/1", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps/1", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123/steps/1", null, null },

				/* JobStepExecutionProgressController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps/1/progress", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/123/steps/1/progress", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/123/steps/1/progress", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/123/steps/1/progress", null, null },

				/* RuntimeAppsController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/runtime/apps", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/runtime/apps", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/runtime/apps/123", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/runtime/apps/123", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123/instances", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/runtime/apps/123/instances", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123/instances", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/runtime/apps/123/instances", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123/instances/456", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/runtime/apps/123/instances/456", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/apps/123/instances/456", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/runtime/apps/123/instances/456", null, null },

				/* StreamDefinitionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/streams/definitions", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", manageOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.OK, "/streams/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", createOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions", null,
						TestUtils.toImmutableMap("page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", manageOnlyUser,
						TestUtils.toImmutableMap("findByTaskNameContains", "mysearch") },
				{ HttpMethod.GET, HttpStatus.OK, "/streams/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("findByTaskNameContains", "mysearch") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", createOnlyUser,
						TestUtils.toImmutableMap("findByTaskNameContains", "mysearch") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions", null,
						TestUtils.toImmutableMap("findByTaskNameContains", "mysearch") },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/definitions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "definition", "fooo | baaar") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "definition", "fooo | baaar") },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/streams/definitions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname", "definition", "fooo | baaar") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/streams/definitions", null,
						TestUtils.toImmutableMap("name", "myname", "definition", "fooo | baaar") },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/definitions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/streams/definitions", createOnlyUser,
						TestUtils.toImmutableMap("name", "myname") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/streams/definitions", null,
						TestUtils.toImmutableMap("name", "myname") },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions/delete-me", manageOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions/delete-me", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/streams/definitions/delete-me", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/streams/definitions/delete-me", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream/related", manageOnlyUser,
						null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/streams/definitions/my-stream/related", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream/related", createOnlyUser,
						null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions/my-stream/related", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream/related", manageOnlyUser,
						TestUtils.toImmutableMap("nested", "wrong-param") },
				{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/streams/definitions/my-stream/related", viewOnlyUser,
						TestUtils.toImmutableMap("nested", "wrong-param") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream/related", createOnlyUser,
						TestUtils.toImmutableMap("nested", "wrong-param") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions/my-stream/related", null,
						TestUtils.toImmutableMap("nested", "wrong-param") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/streams/definitions/my-stream", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions/my-stream", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/definitions/my-stream", null, null },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions", manageOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.OK, "/streams/definitions", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/streams/definitions", null, null },

				/* SkipperStreamDeploymentController */

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments", manageOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.OK, "/streams/deployments", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/streams/deployments", null, null },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments/my-stream", manageOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments/my-stream", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/streams/deployments/my-stream", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/streams/deployments/my-stream", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/deployments/my-stream", manageOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/deployments/my-stream", viewOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/streams/deployments/my-stream", createOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/streams/deployments/my-stream", null, null },

				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/deployments/history/my-stream/2", null, null },

				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/deployments/manifest/my-stream/2", null, null },

				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/streams/deployments/platform/list", null, null },

				/* TaskPlatformController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/platforms", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/tasks/platforms", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/platforms", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tasks/platforms", null, null },

				/* TaskDefinitionController */

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/definitions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "my-name") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-name") },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/tasks/definitions", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-name") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/tasks/definitions", null,
						TestUtils.toImmutableMap("name", "my-name") },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/definitions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "my-name", "definition", "foo") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/definitions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-name", "definition", "foo") },
				{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/tasks/definitions", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-name", "definition", "foo") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/tasks/definitions", null,
						TestUtils.toImmutableMap("name", "my-name", "definition", "foo") },

				/* TaskExecutionController */

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/tasks/executions", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tasks/executions", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", manageOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.OK, "/tasks/executions", viewOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", createOnlyUser,
						TestUtils.toImmutableMap("page", "0", "size", "10") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tasks/executions", null,
						TestUtils.toImmutableMap("page", "0", "size", "10") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/tasks/executions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tasks/executions", null,
						TestUtils.toImmutableMap("name", "my-task-name") },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions/123", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/tasks/executions/123", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/tasks/executions/123", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/tasks/executions/123", null, null },

				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/tasks/executions/123", manageOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/tasks/executions/123", viewOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/tasks/executions/123", createOnlyUser, null },
				{ HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/tasks/executions/123", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions", manageOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions", viewOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.BAD_REQUEST, "/tasks/executions", createOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/tasks/executions", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions/123", manageOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions/123", viewOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/tasks/executions/123", createOnlyUser, null },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/tasks/executions/123", null, null },

				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions", manageOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/tasks/executions", viewOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/tasks/executions", createOnlyUser,
						TestUtils.toImmutableMap("name", "my-task-name") },
				{ HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/tasks/executions", null,
						TestUtils.toImmutableMap("name", "my-task-name") },

				/* UiController */

				{ HttpMethod.GET, HttpStatus.FOUND, "/dashboard", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FOUND, "/dashboard", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FOUND, "/dashboard", createOnlyUser, null },

				// FIXME
				// { HttpMethod.GET, HttpStatus.FOUND, "/dashboard", null, null },

				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/about", manageOnlyUser, null },

				{ HttpMethod.GET, HttpStatus.OK, "/about", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/about", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/about", null, null },

				{ HttpMethod.GET, HttpStatus.OK, "/management", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/management", null, null },

				{ HttpMethod.GET, HttpStatus.OK, "/management/info", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/management/info", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/management/info", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/management/info", null, null },

				{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/management/does-not-exist", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management/does-not-exist", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/management/does-not-exist", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/management/does-not-exist", null, null },

				/* SecurityController */

				{ HttpMethod.GET, HttpStatus.OK, "/security/info", manageOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/security/info", viewOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/security/info", createOnlyUser, null },
				{ HttpMethod.GET, HttpStatus.OK, "/security/info", null, null }
		});
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

	private static class UserCredentials {
		final String username;
		final String password;

		public UserCredentials(String username, String password) {
			super();
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		@Override
		public String toString() {
			return String.format("UserCredentials: %s/%s.", this.username, this.password);
		}

	}
}
