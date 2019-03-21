/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.rest.documentation;

import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.server.local.LocalDataflowResource;
import org.springframework.cloud.skipper.server.local.security.LocalSkipperResource;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.SocketUtils;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilayaperumal Gopinathan
 */
public abstract class BaseSkipperDocumentation {


	private static LocalDataflowResource springDataflowServer;

	private static LocalSkipperResource skipperServer;

	private static String skipperServerPort;

	@ClassRule
	public static LocalSkipperResource setupSkipperServer() {
		skipperServerPort = String.valueOf(SocketUtils.findAvailableTcpPort());
		skipperServer = new LocalSkipperResource(
				new String[] {"classpath:rest-docs-config-skipper.yml"}, null,
				new String[] {"--server.port="+ skipperServerPort});
		return skipperServer;
	}

	@ClassRule
	public static LocalDataflowResource setupSCDFAndSkipperServer() {
		springDataflowServer = new LocalDataflowResource(
				"classpath:rest-docs-config-scdf-skipper-mode.yml", true, true, true, true, true, skipperServerPort);
		return springDataflowServer;
	}

	@Before
	public void setupMocks() {
		this.prepareDocumentationTests(springDataflowServer.getWebApplicationContext());
	}

	public static final String TARGET_DIRECTORY = "target/generated-snippets";

	@Rule
	public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation(TARGET_DIRECTORY);

	protected MockMvc mockMvc;

	protected RestDocumentationResultHandler documentationHandler;

	protected RestDocs documentation;

	protected void prepareDocumentationTests(WebApplicationContext context) {
		this.documentationHandler = document("{class-name}/{method-name}", preprocessResponse(prettyPrint()));
		this.documentation = new ToggleableResultHandler(documentationHandler);

		this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(documentationConfiguration(this.restDocumentation).uris().withPort(9393))
				.alwaysDo((ToggleableResultHandler)this.documentation).build();
	}

	/**
	 * Can be used by subclasses to easily register dummy apps, as most endpoints require apps to be effective
	 * @param type the type of app to register
	 * @param name the name of the app to register
	 * @param version the version to register
	 */
	void registerApp(ApplicationType type, String name, String version) throws Exception {
		String group = type == ApplicationType.task ? "org.springframework.cloud.task.app" : "org.springframework.cloud.stream.app";
		String binder = type == ApplicationType.task ? "" : "-rabbit";

		documentation.dontDocument(
				() -> this.mockMvc.perform(
						post(String.format("/apps/%s/%s/%s", type, name, version))
								.param("uri", String.format("maven://%s:%s-%s%s:%s", group, name, type, binder, version)))
						.andExpect(status().isCreated())
		);
	}

	void unregisterApp(ApplicationType type, String name) throws Exception {
		documentation.dontDocument(
				() -> this.mockMvc.perform(
						delete(String.format("/apps/%s/%s", type, name))
				)
						.andExpect(status().isOk())
		);
	}

	/**
	 * A {@link ResultHandler} that can be turned off and on.
	 *
	 * @author Eric Bottard
	 */
	private static class ToggleableResultHandler implements ResultHandler, RestDocs {
		private final ResultHandler delegate;

		private boolean off = false;

		private ToggleableResultHandler(ResultHandler delegate) {
			this.delegate = delegate;
		}

		@Override
		public void handle(MvcResult result) throws Exception {
			if (!off) {
				delegate.handle(result);
			}
		}

		/**
		 * Perform the given action while turning off the delegate handler.
		 */
		@Override
		public void dontDocument(Callable action) throws Exception {
			off = true;
			try {
				action.call();
			} finally {
				off = false;
			}
		}
	}

	/**
	 * Functional interface allowing to silence the Spring Rest Docs handler, so that setUp / tearDown actions
	 * are not documented.
	 *
	 * @author Eric Bottard
	 */
	@FunctionalInterface
	public interface RestDocs {
		void dontDocument(Callable action) throws Exception;
	}
}
