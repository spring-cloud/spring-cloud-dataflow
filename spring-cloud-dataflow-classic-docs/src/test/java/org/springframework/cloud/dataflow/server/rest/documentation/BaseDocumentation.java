/*
 * Copyright 2016-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.controller.TaskSchedulerController;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.single.LocalDataflowResource;
import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.domain.Dependency;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.VersionInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@ExtendWith(RestDocumentationExtension.class)
public abstract class BaseDocumentation {

	private static String skipperServerPort;

	@RegisterExtension
	public final static LocalDataflowResource springDataflowServer = new LocalDataflowResource(
		"classpath:rest-docs-config.yml", true, true, true, true, skipperServerPort);

	@BeforeEach
	public void setupMocks(RestDocumentationContextProvider restDocumentationContextProvider) throws Exception {
		reset(springDataflowServer.getSkipperClient());

		AboutResource about = new AboutResource();
		about.setVersionInfo(new VersionInfo());
		about.getVersionInfo().setServer(new Dependency());
		about.getVersionInfo().getServer().setName("Test Server");
		about.getVersionInfo().getServer().setVersion("Test Version");
		when(springDataflowServer.getSkipperClient().info()).thenReturn(about);
		when(springDataflowServer.getSkipperClient().listDeployers()).thenReturn(new ArrayList<>());

		Info info = new Info();
		info.setStatus(new Status());
		info.getStatus().setStatusCode(StatusCode.UNKNOWN);
		when(springDataflowServer.getSkipperClient().status(ArgumentMatchers.anyString())).thenReturn(info);

		Deployer deployer = new Deployer("default", "local", mock(AppDeployer.class), mock(ActuatorOperations.class));
		when(springDataflowServer.getSkipperClient().listDeployers()).thenReturn(Collections.singletonList(deployer));

		when(springDataflowServer.getSkipperClient().search(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean())).thenReturn(new ArrayList<>());

		this.prepareDocumentationTests(springDataflowServer.getWebApplicationContext(),
			restDocumentationContextProvider);
	}

	public static final String TARGET_DIRECTORY = "target/generated-snippets";

	protected MockMvc mockMvc;

	protected RestDocumentationResultHandler documentationHandler;

	protected RestDocs documentation;

	protected DataSource dataSource;

	protected ApplicationContext context;

	protected void prepareDocumentationTests(WebApplicationContext context,
											 RestDocumentationContextProvider restDocumentationContextProvider) {
		this.context = context;
		this.documentationHandler = document("{class-name}/{method-name}", preprocessResponse(prettyPrint()));
		this.documentation = new ToggleableResultHandler(documentationHandler);

		this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(documentationConfiguration(restDocumentationContextProvider).uris().withPort(9393))
			.alwaysDo((ToggleableResultHandler) this.documentation).build();

		this.dataSource = springDataflowServer.getWebApplicationContext().getBean(DataSource.class);
		TaskSchedulerController controller = springDataflowServer.getWebApplicationContext()
			.getBean(TaskSchedulerController.class);
		ReflectionTestUtils.setField(controller, "schedulerService", schedulerService());
		TaskPlatform taskPlatform = springDataflowServer.getWebApplicationContext().getBean(TaskPlatform.class);
		Launcher launcher = taskPlatform.getLaunchers().stream().filter(launcherToFilter -> launcherToFilter.getName().equals("default")).findFirst().get();
		ReflectionTestUtils.setField(launcher, "scheduler", localTestScheduler());
	}

	/**
	 * Can be used by subclasses to easily register dummy apps, as most endpoints require apps to be effective
	 *
	 * @param type    the type of app to register
	 * @param name    the name of the app to register
	 * @param version the version to register
	 */
	void registerApp(ApplicationType type, String name, String version) throws Exception {
		String group = type == ApplicationType.task ? "io.spring" : "org.springframework.cloud.stream.app";
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

	void unregisterApp(ApplicationType type, String name, String version) throws Exception {
		documentation.dontDocument(
				() -> this.mockMvc.perform(
								delete(String.format("/apps/%s/%s/%s", type, name, version))
						)
						.andExpect(status().isOk())
		);
	}

	void createStream(String name, String definition, boolean deploy) throws Exception {
		documentation.dontDocument(
				() -> this.mockMvc.perform(
								post("/streams/definitions")
										.param("name", name)
										.param("definition", definition)
										.param("deploy", String.valueOf(deploy)))
						.andExpect(status().isCreated())
		);
	}

	void destroyStream(String name) throws Exception {
		documentation.dontDocument(
				() -> this.mockMvc.perform(
								delete("/streams/definitions/{name}", name))
						.andExpect(status().isOk())
		);
	}

	/**
	 * A {@link ResultHandler} that can be turned off and on.
	 *
	 * @author Eric Bottard
	 * @author Corneil du Plessis
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
	 * @author Corneil du Plessis
	 */
	@FunctionalInterface
	public interface RestDocs {
		void dontDocument(Callable action) throws Exception;
	}

	public SchedulerService schedulerService() {
		return new SchedulerService() {
			@Override
			public void schedule(String scheduleName, String taskDefinitionName,
								 Map<String, String> taskProperties, List<String> commandLineArgs,
								 String platformName) {
			}

			@Override
			public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskProperties, List<String> commandLineArgs) {

			}

			@Override
			public void unschedule(String scheduleName) {

			}

			@Override
			public void unschedule(String scheduleName, String platformName) {
			}

			@Override
			public void unscheduleForTaskDefinition(String taskDefinitionName) {
			}

			@Override
			public List<ScheduleInfo> list(Pageable pageable, String taskDefinitionName,
										   String platformName) {
				return null;
			}

			@Override
			public Page<ScheduleInfo> list(Pageable pageable, String platformName) {
				return null;
			}

			@Override
			public Page<ScheduleInfo> list(Pageable pageable) {
				return null;
			}

			@Override
			public List<ScheduleInfo> list(String taskDefinitionName, String platformName) {
				return getSampleList();
			}

			@Override
			public List<ScheduleInfo> list(String taskDefinitionName) {
				return getSampleList();
			}

			@Override
			public List<ScheduleInfo> listForPlatform(String platformName) {
				return getSampleList();
			}

			@Override
			public List<ScheduleInfo> list() {
				return null;
			}

			@Override
			public ScheduleInfo getSchedule(String scheduleName, String platformName) {
				return null;
			}

			@Override
			public ScheduleInfo getSchedule(String scheduleName) {
				return null;
			}
		};
	}

	private List<ScheduleInfo> getSampleList() {
		List<ScheduleInfo> result = new ArrayList<>();
		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName("FOO");
		scheduleInfo.setTaskDefinitionName("BAR");
		Map<String, String> props = new HashMap<>(1);
		props.put("scheduler.AAA.spring.cloud.scheduler.cron.expression", "00 41 17 ? * *");
		scheduleInfo.setScheduleProperties(props);
		result.add(scheduleInfo);
		return result;
	}

	public Scheduler localTestScheduler() {
		return new Scheduler() {
			@Override
			public void schedule(ScheduleRequest scheduleRequest) {
				throw new UnsupportedOperationException("Interface is not implemented for schedule method.");
			}

			@Override
			public void unschedule(String scheduleName) {
				throw new UnsupportedOperationException("Interface is not implemented for unschedule method.");
			}

			@Override
			public List<ScheduleInfo> list(String taskDefinitionName) {
				throw new UnsupportedOperationException("Interface is not implemented for list method.");
			}

			@Override
			public List<ScheduleInfo> list() {
				return getSampleList();
			}
		};
	}
}
