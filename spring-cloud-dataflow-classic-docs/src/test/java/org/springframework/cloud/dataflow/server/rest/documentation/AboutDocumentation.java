/*
 * Copyright 2017-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Chris Bono
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
class AboutDocumentation extends BaseDocumentation {

	@Test
	void getMetaInformation() throws Exception {
		this.mockMvc.perform(
			get("/about")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(responseFields(
						fieldWithPath("_links.self.href").description("Link to the runtime environment resource"),

						fieldWithPath("featureInfo").type(JsonFieldType.OBJECT)
								.description("Details which features are enabled."),
						fieldWithPath("featureInfo.analyticsEnabled").type(JsonFieldType.BOOLEAN)
								.description("Describes if analytics feature is enabled."),
						fieldWithPath("featureInfo.schedulesEnabled").type(JsonFieldType.BOOLEAN)
								.description("Describes if scheduler feature is enabled."),
						fieldWithPath("featureInfo.streamsEnabled").type(JsonFieldType.BOOLEAN)
								.description("Describes if streams feature is enabled."),
						fieldWithPath("featureInfo.tasksEnabled").type(JsonFieldType.BOOLEAN)
								.description("Describes if tasks feature is enabled."),
						fieldWithPath("featureInfo.monitoringDashboardType").type(JsonFieldType.STRING)
								.description("Name of the external metrics dashboards if provided or none otherwise."),

						fieldWithPath("versionInfo").type(JsonFieldType.OBJECT).description(
								"Provides details of the Spring Cloud Data Flow Server " + "dependencies."),
						fieldWithPath("versionInfo.implementation").type(JsonFieldType.OBJECT).description(
								"Provides details of the Spring Cloud Data Flow Server implementation dependency."),
						fieldWithPath("versionInfo.implementation.name").type(JsonFieldType.STRING).description(
								"Provides details of the Spring Cloud Data Flow Server implementation name."),
						fieldWithPath("versionInfo.implementation.version").type(JsonFieldType.STRING).description(
								"Provides details of the Spring Cloud Data Flow Server implementation version."),

						fieldWithPath("versionInfo.core").type(JsonFieldType.OBJECT).description(
								"Provides details of the core dependency."),
						fieldWithPath("versionInfo.core.name").type(JsonFieldType.STRING).description(
								"Provides details of the core name."),
						fieldWithPath("versionInfo.core.version").type(JsonFieldType.STRING).description(
								"Provides details of the core version."),

						fieldWithPath("versionInfo.dashboard").type(JsonFieldType.OBJECT).description(
								"Provides details of the dashboard dependency."),
						fieldWithPath("versionInfo.dashboard.name").type(JsonFieldType.STRING).description(
								"Provides details of the dashboard name."),
						fieldWithPath("versionInfo.dashboard.version").type(JsonFieldType.STRING).description(
								"Provides details of the dashboard version."),

						fieldWithPath("versionInfo.shell").type(JsonFieldType.OBJECT).description(
								"Provides details of the shell dependency."),
						fieldWithPath("versionInfo.shell.name").type(JsonFieldType.STRING).description(
								"Provides details of the shell name."),
						fieldWithPath("versionInfo.shell.version").type(JsonFieldType.STRING).description(
								"Provides details of the shell version."),
						fieldWithPath("versionInfo.shell.url").type(JsonFieldType.STRING).description(
								"Provides details of the shell url."),

						fieldWithPath("securityInfo").type(JsonFieldType.OBJECT)
								.description("Provides security configuration information."),
						fieldWithPath("securityInfo.authenticationEnabled").type(JsonFieldType.BOOLEAN)
								.description("Describes if security authentication is enabled."),
						fieldWithPath("securityInfo.authenticated").type(JsonFieldType.BOOLEAN)
								.description("Describes if user is authenticated."),
						fieldWithPath("securityInfo.username").type(JsonFieldType.STRING).optional()
								.description("Describes current username."),
						fieldWithPath("securityInfo.roles").type(JsonFieldType.ARRAY)
								.description("Describes current roles."),

						fieldWithPath("runtimeEnvironment").type(JsonFieldType.OBJECT)
								.description("Provides details of the runtime environment."),
						fieldWithPath("runtimeEnvironment.appDeployer").type(JsonFieldType.OBJECT)
								.description("Provides details of the appDeployer environment."),
						fieldWithPath("runtimeEnvironment.appDeployer.deployerImplementationVersion").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer implementation version."),
						fieldWithPath("runtimeEnvironment.appDeployer.deployerName").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer deployer name."),
						fieldWithPath("runtimeEnvironment.appDeployer.deployerSpiVersion").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer deployer SPI version."),
						fieldWithPath("runtimeEnvironment.appDeployer.javaVersion").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer java version."),
						fieldWithPath("runtimeEnvironment.appDeployer.platformApiVersion").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer platform api version."),
						fieldWithPath("runtimeEnvironment.appDeployer.platformClientVersion").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer platform client version."),
						fieldWithPath("runtimeEnvironment.appDeployer.platformHostVersion").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer platform host version."),
						fieldWithPath("runtimeEnvironment.appDeployer.platformSpecificInfo").type(JsonFieldType.OBJECT)
								.description("Provides details of the appDeployer specific info."),
						fieldWithPath("runtimeEnvironment.appDeployer.platformSpecificInfo.default").type(JsonFieldType.STRING)
								.description("Provides details of the name of appDeployer used by the platformSpecificInfo."),
						fieldWithPath("runtimeEnvironment.appDeployer.platformType").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer platform type."),
						fieldWithPath("runtimeEnvironment.appDeployer.springBootVersion").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer boot version."),
						fieldWithPath("runtimeEnvironment.appDeployer.springVersion").type(JsonFieldType.STRING)
								.description("Provides details of the appDeployer spring version."),

						fieldWithPath("runtimeEnvironment.taskLaunchers").type(JsonFieldType.ARRAY)
								.description("Provides details of the taskLauncher environment."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].deployerImplementationVersion").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher implementation version."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].deployerName").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher deployer name."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].deployerSpiVersion").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher deployer SPI version."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].javaVersion").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher java version."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].platformApiVersion").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher platform api version."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].platformClientVersion").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher platform client version."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].platformHostVersion").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher platform host version."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].platformSpecificInfo").type(JsonFieldType.OBJECT)
								.description("Provides details of the taskLauncher specific info."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].platformType").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher platform type."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].springBootVersion").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher boot version."),
						fieldWithPath("runtimeEnvironment.taskLaunchers[0].springVersion").type(JsonFieldType.STRING)
								.description("Provides details of the taskLauncher spring version."),
						fieldWithPath("monitoringDashboardInfo").type(JsonFieldType.OBJECT).description(
								"Provides details of how to access the Monitoring Dashboards if provided."),
						fieldWithPath("monitoringDashboardInfo.url").type(JsonFieldType.STRING).description(
								"Provides URL of the external monitoring dashboard."),
						fieldWithPath("monitoringDashboardInfo.dashboardType").type(JsonFieldType.STRING).description(
								"Type of the monitoring dashboard system."),
						fieldWithPath("monitoringDashboardInfo.source").type(JsonFieldType.STRING).description(
								"Unique DataFlow identifier within the monitoring system."),
						fieldWithPath("monitoringDashboardInfo.refreshInterval").type(JsonFieldType.NUMBER).description(
								"Provides the time interval (in seconds) for updating the monitoring dashboards."),

						fieldWithPath("gitAndBuildInfo").type(JsonFieldType.OBJECT).description(
								"Provides the git and build info for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.git").type(JsonFieldType.OBJECT).description(
								"Provides the git details for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.git.branch").type(JsonFieldType.STRING).description(
								"Provides the git branch for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.git.commit").type(JsonFieldType.OBJECT).description(
								"Provides the git commit info for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.git.commit.id").type(JsonFieldType.OBJECT).description(
								"Provides the git commit id for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.git.commit.id.abbrev").type(JsonFieldType.STRING).description(
							"Provides the short git commit id for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.git.commit.id.full").type(JsonFieldType.STRING).description(
							"Provides the full git commit id for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.git.commit.time").type(JsonFieldType.STRING).description(
								"Provides the git commit time for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.build").type(JsonFieldType.OBJECT).description(
								"Provides the build details for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.build.artifact").type(JsonFieldType.STRING).description(
								"Provides the build artifact for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.build.name").type(JsonFieldType.STRING).description(
								"Provides the build name for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.build.time").type(JsonFieldType.STRING).description(
								"Provides the build time for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.build.version").type(JsonFieldType.STRING).description(
								"Provides the build version for the Dataflow server"),
						fieldWithPath("gitAndBuildInfo.build.group").type(JsonFieldType.STRING).description(
								"Provides the build group for the Dataflow server")
				)));
	}
}
