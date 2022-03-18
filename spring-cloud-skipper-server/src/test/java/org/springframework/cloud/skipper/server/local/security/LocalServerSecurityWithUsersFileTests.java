/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.cloud.skipper.server.local.security;

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

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.CollectionUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for security configuration backed by a file-based user list.
 *
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@RunWith(Parameterized.class)
public class LocalServerSecurityWithUsersFileTests {

	private final static Logger logger = LoggerFactory.getLogger(LocalServerSecurityWithUsersFileTests.class);

	private final static OAuth2ServerResource oAuth2ServerResource = new OAuth2ServerResource();

	private final static LocalSkipperResource localSkipperResource = new LocalSkipperResource(
			new String[] {
					"optional:classpath:/",
					"optional:classpath:/org/springframework/cloud/skipper/server/local/security/"
			},
			new String[] {
					"application",
					"oauthConfig"
			}
	);

	@ClassRule
	public static TestRule skipperAndOAuth2Server = RuleChain.outerRule(oAuth2ServerResource)
			.around(localSkipperResource);

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
		//localSkipperResource.mockSkipperAboutInfo();
	}

	@Parameters(name = "Authentication Test {index} - {0} {2} - Returns: {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {

				/* AboutController */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/about", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.OK, "/api/about", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/about", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/about", null, null},

				/* Deployers */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/deployers", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.OK, "/api/deployers", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/deployers", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/deployers", null, null},

				/* Releases */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/releases", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.OK, "/api/releases", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/releases", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/releases", null, null},

				/* Status */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/status/does_not_exist", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.NOT_FOUND, "/api/release/status/does_not_exist", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/status/does_not_exist", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/release/status/does_not_exist", null, null},

				/* Manifest */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/manifest/does_not_exist", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.NOT_FOUND, "/api/release/manifest/does_not_exist", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/manifest/does_not_exist", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/release/manifest/does_not_exist", null, null},

				/* Logs */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/logs/does_not_exist", manageOnlyUser, null},
				// { HttpMethod.GET, HttpStatus.NOT_FOUND,    "/api/release/logs/does_not_exist", viewOnlyUser, null }, // GH-906
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/logs/does_not_exist", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/release/logs/does_not_exist", null, null},

				/* Upgrade */

				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/release/upgrade", manageOnlyUser, null},
				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/release/upgrade", viewOnlyUser, null},
				{HttpMethod.POST, HttpStatus.BAD_REQUEST, "/api/release/upgrade", createOnlyUser, null},
				{HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/api/release/upgrade", null, null},

				/* Rollback */

				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/release/rollback", manageOnlyUser, null},
				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/release/rollback", viewOnlyUser, null},
				{HttpMethod.POST, HttpStatus.BAD_REQUEST, "/api/release/rollback", createOnlyUser, null},
				{HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/api/release/rollback", null, null},

				/* Rollback with Name */

				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/release/rollback/does_not_exist", manageOnlyUser, null},
				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/release/rollback/does_not_exist", viewOnlyUser, null},
				{HttpMethod.POST, HttpStatus.NOT_FOUND, "/api/release/rollback/does_not_exist", createOnlyUser, null},
				{HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/api/release/rollback/does_not_exist", null, null},

				/* Delete */

				{HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/api/release/does_not_exist", manageOnlyUser, null},
				{HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/api/release/does_not_exist", viewOnlyUser, null},
				{HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/api/release/does_not_exist", createOnlyUser, null},
				{HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/api/release/does_not_exist", null, null},

				/* Delete with Package*/

				{HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/api/release/does_not_exist/package", manageOnlyUser, null},
				{HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/api/release/does_not_exist/package", viewOnlyUser, null},
				{HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/api/release/does_not_exist/package", createOnlyUser, null},
				{HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/api/release/does_not_exist/package", null, null},

				/* History */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/history/does_not_exist", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.NOT_FOUND, "/api/release/history/does_not_exist", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/history/does_not_exist", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/release/history/does_not_exist", null, null},

				/* List */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/list", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.OK, "/api/release/list", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/list", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/release/list", null, null},

				/* List by Release Name */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/list/does_not_exist", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.OK, "/api/release/list/does_not_exist", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/release/list/does_not_exist", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/release/list/does_not_exist", null, null},

				/* Package */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/package", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.OK, "/api/package", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/package", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/package", null, null},

				/* Upload */

				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/package/upload", manageOnlyUser, null},
				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/package/upload", viewOnlyUser, null},
				{HttpMethod.POST, HttpStatus.BAD_REQUEST, "/api/package/upload", createOnlyUser, null},
				{HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/api/package/upload", null, null},

				/* Install */


				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/package/install", manageOnlyUser, null},
				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/package/install", viewOnlyUser, null},
				{HttpMethod.POST, HttpStatus.BAD_REQUEST, "/api/package/install", createOnlyUser, null},
				{HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/api/package/install", null, null},

				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/package/install/123", manageOnlyUser, null},
				{HttpMethod.POST, HttpStatus.FORBIDDEN, "/api/package/install/123", viewOnlyUser, null},
				{HttpMethod.POST, HttpStatus.BAD_REQUEST, "/api/package/install/123", createOnlyUser, null},
				{HttpMethod.POST, HttpStatus.UNAUTHORIZED, "/api/package/install/123", null, null},

				/* Delete */

				{HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/api/package/does_not_exist", manageOnlyUser, null},
				{HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/api/package/does_not_exist", viewOnlyUser, null},
				{HttpMethod.DELETE, HttpStatus.CONFLICT, "/api/package/does_not_exist", createOnlyUser, null},
				{HttpMethod.DELETE, HttpStatus.UNAUTHORIZED, "/api/package/does_not_exist", null, null},

				/* PackageMetaData */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/packageMetadata", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.OK, "/api/packageMetadata", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/packageMetadata", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/packageMetadata", null, null},

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/packageMetadata/123", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.NOT_FOUND, "/api/packageMetadata/123", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/packageMetadata/123", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/packageMetadata/123", null, null},

				/* Repositories */

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/repositories", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.OK, "/api/repositories", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/repositories", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/repositories", null, null},

				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/repositories/123", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.NOT_FOUND, "/api/repositories/123", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/api/repositories/123", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/api/repositories/123", null, null},

				/* Boot Endpoints */

				{HttpMethod.GET, HttpStatus.OK, "/actuator", manageOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/actuator", viewOnlyUser, null},
				{HttpMethod.GET, HttpStatus.FORBIDDEN, "/actuator", createOnlyUser, null},
				{HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/actuator", null, null},

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
					SecurityTestUtils.basicAuthorizationHeader(this.userCredentials.getUsername(), this.userCredentials.getPassword()));
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
		case CONFLICT:
			statusResultMatcher = status().isConflict();
			break;
		case INTERNAL_SERVER_ERROR:
			statusResultMatcher = status().isInternalServerError();
			break;
		default:
			throw new IllegalArgumentException("Unsupported Status: " + expectedHttpStatus);
		}

		try {
			localSkipperResource.getMockMvc().perform(rb).andDo(print())
					.andExpect(statusResultMatcher);
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

		UserCredentials(String username, String password) {
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
			return "username = " + this.username;
		}
	}
}
