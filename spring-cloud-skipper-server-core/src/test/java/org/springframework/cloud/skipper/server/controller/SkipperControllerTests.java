/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.server.controller;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import org.junit.Test;

import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "spring.cloud.skipper.server.platform.local.accounts[test].key=value",
		"maven.remote-repositories.repo1.url=http://repo.spring.io/libs-snapshot",
		"spring.cloud.skipper.server.disableReleaseStateUpdateService=true" })
public class SkipperControllerTests extends AbstractControllerTests {

	@Test
	public void deployTickTock() throws Exception {
		String releaseName = "myTicker";
		Release release = deploy("ticktock", "1.0.0", "myTicker");
		assertReleaseIsDeployedSuccessfully(releaseName, "1");
		assertThat(release.getVersion()).isEqualTo(1);
	}

	@Test
	public void packageDeployRequest() throws Exception {
		String releaseName = "myLogRelease";
		InstallRequest installRequest = new InstallRequest();
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		packageIdentifier.setRepositoryName("notused");
		installRequest.setPackageIdentifier(packageIdentifier);
		InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName(releaseName);
		installProperties.setPlatformName("test");
		installRequest.setInstallProperties(installProperties);

		Release release = installPackage(installRequest);
		assertReleaseIsDeployedSuccessfully(releaseName, "1");
		assertThat(release.getVersion()).isEqualTo(1);
	}

	@Test
	public void checkDeployStatus() throws Exception {

		// Deploy
		String releaseName = "test1";
		Release release = deploy("log", "1.0.0", releaseName);
		assertReleaseIsDeployedSuccessfully(releaseName, "1");
		assertThat(release.getVersion()).isEqualTo(1);

		// Undeploy
		mockMvc.perform(post("/api/delete/" + releaseName)).andDo(print())
				.andExpect(status().isCreated()).andReturn();
		Release deletedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 1);
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DELETED);
	}

	@Test
	public void releaseRollbackAndUndeploy() throws Exception {

		// Deploy
		String releaseName = "test2";
		Release release = deploy("log", "1.0.0", releaseName);
		assertReleaseIsDeployedSuccessfully(releaseName, "1");
		assertThat(release.getVersion()).isEqualTo(1);

		// Check manifest
		MvcResult result = mockMvc.perform(get("/api/manifest/" + releaseName)).andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertThat(result.getResponse().getContentAsString()).isNotEmpty();

		// Update
		String releaseVersion = "2";
		release = upgrade("log", "1.1.0", releaseName);
		assertReleaseIsDeployedSuccessfully(releaseName, releaseVersion);
		assertThat(release.getVersion()).isEqualTo(2);

		// Check manifest
		result = mockMvc.perform(get("/api/manifest/" + releaseName + "/2")).andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertThat(result.getResponse().getContentAsString()).isNotEmpty();

		// Rollback to release version 1, creating a third release version equivalent to
		// the 1st.
		releaseVersion = "3";
		mockMvc.perform(post("/api/rollback/" + releaseName + "/" + 1)).andDo(print())
				.andExpect(status().isCreated()).andReturn();
		sleep();

		release = this.releaseRepository.findByNameAndVersion(releaseName, Integer.valueOf(releaseVersion));
		assertReleaseIsDeployedSuccessfully(releaseName, "3");

		// TODO the common assert doesn't check for this status code.
		assertThat(release.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DEPLOYED);

		// Undeploy
		mockMvc.perform(post("/api/delete/" + releaseName))
				.andDo(print())
				.andExpect(status().isCreated()).andReturn();
		Release deletedRelease = this.releaseRepository.findByNameAndVersion(releaseName,
				Integer.valueOf(releaseVersion));
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DELETED);
	}

	@Test
	public void packageDeployAndUpgrade() throws Exception {
		String releaseName = "myLog";
		Release release = deploy("log", "1.0.0", releaseName);
		assertReleaseIsDeployedSuccessfully(releaseName, "1");
		assertThat(release.getVersion()).isEqualTo(1);

		// Upgrade
		release = upgrade("log", "1.1.0", releaseName);
		assertReleaseIsDeployedSuccessfully(releaseName, "2");
		assertThat(release.getVersion()).isEqualTo(2);
	}

	@Test
	public void testStatusReportsErrorForMissingRelease() throws Exception {
		// In a real container the response is carried over into the error dispatcher, but
		// in the mock a new one is created so we have to assert the status at this
		// intermediate point
		MvcResult result = mockMvc.perform(get("/api/status/myLog")).andDo(print())
				.andExpect(status().is4xxClientError()).andReturn();
		MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error"))
				.andReturn();
		assertThat(response.getResponse().getContentAsString()).contains("ReleaseNotFoundException");
	}

	private class ErrorDispatcher implements RequestBuilder {

		private MvcResult result;

		private String path;

		ErrorDispatcher(MvcResult result, String path) {
			this.result = result;
			this.path = path;
		}

		@Override
		public MockHttpServletRequest buildRequest(ServletContext servletContext) {
			MockHttpServletRequest request = this.result.getRequest();
			request.setDispatcherType(DispatcherType.ERROR);
			request.setRequestURI(this.path);
			return request;
		}
	}
}
