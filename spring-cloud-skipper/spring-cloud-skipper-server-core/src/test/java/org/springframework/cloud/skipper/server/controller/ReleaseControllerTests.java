/*
 * Copyright 2017-2024 the original author or authors.
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
package org.springframework.cloud.skipper.server.controller;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.deployer.DefaultReleaseManager;
import org.springframework.cloud.skipper.server.repository.jpa.RepositoryRepository;
import org.springframework.cloud.skipper.server.service.ActuatorService;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author David Turanski
 * @author Corneil du Plessis
 */
@ActiveProfiles({"repo-test", "local"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReleaseControllerTests extends AbstractControllerTests {

	@MockBean
	private ActuatorService actuatorService;

	@Autowired
	private RepositoryRepository repositoryRepository;

	@Test
	public void deployTickTock() throws Exception {
		Release release = install("ticktock", "4.0.0", "myTicker");
		assertReleaseIsDeployedSuccessfully("myTicker", 1);
		assertThat(release.getVersion()).isEqualTo(1);
	}

	@Test
	public void packageDeployRequest() throws Exception {
		String releaseName = "myLogRelease";
		InstallRequest installRequest = new InstallRequest();
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("4.0.0");
		packageIdentifier.setRepositoryName("notused");
		installRequest.setPackageIdentifier(packageIdentifier);
		InstallProperties installProperties = createInstallProperties(releaseName);
		installRequest.setInstallProperties(installProperties);

		Release release = installPackage(installRequest);
		assertReleaseIsDeployedSuccessfully(releaseName, 1);
		assertThat(release.getVersion()).isEqualTo(1);
	}

	@Test
	public void checkDeployStatus() throws Exception {

		// Deploy
		String releaseName = "test1";
		Release release = install("log", "4.0.0", releaseName);
		assertThat(release.getVersion()).isEqualTo(1);

		// Undeploy
		mockMvc.perform(delete("/api/release/" + releaseName)).andDo(print())
				.andExpect(status().isOk()).andReturn();
		Release deletedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 1);
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DELETED);
	}

	@Test
	public void getReleaseLogs() throws Exception {
		// Deploy
		String releaseName = "testLogs";
		install("log", "4.0.0", releaseName);
		MvcResult result = mockMvc.perform(get("/api/release/logs/" + releaseName)).andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertThat(result.getResponse().getContentAsString()).isNotEmpty();
	}


	@Test
	public void checkDeleteReleaseWithPackage() throws Exception {

		// Make the test repo Local
		Repository repo = this.repositoryRepository.findByName("test");
		repo.setLocal(true);
		this.repositoryRepository.save(repo);

		// Deploy
		String releaseNameOne = "test1";
		Release release = install("log", "4.0.0", releaseNameOne);
		assertThat(release.getVersion()).isEqualTo(1);

		String releaseNameTwo = "test2";
		Release release2 = install("log", "4.0.0", releaseNameTwo);
		assertThat(release2.getVersion()).isEqualTo(1);

		// Undeploy
		MvcResult result = mockMvc.perform(delete("/api/release/" + releaseNameOne + "/package"))
				.andDo(print()).andExpect(status().isConflict()).andReturn();

		assertThat(result.getResolvedException().getMessage())
				.contains("Can not delete Package Metadata [log:4.0.0] in Repository [test]. Not all releases of " +
						"this package have the status DELETED. Active Releases [test2]");

		assertThat(this.packageMetadataRepository.findByName("log")).hasSize(5);

		// Delete the 'release2' only not the package.
		mockMvc.perform(delete("/api/release/" + releaseNameTwo))
				.andDo(print()).andExpect(status().isOk()).andReturn();
		assertThat(this.packageMetadataRepository.findByName("log")).hasSize(5);

		// Second attempt to delete 'release1' along with its package 'log'.
		mockMvc.perform(delete("/api/release/" + releaseNameOne + "/package"))
				.andDo(print()).andExpect(status().isOk()).andReturn();
		assertThat(this.packageMetadataRepository.findByName("log")).isEmpty();

	}

	@Test
	public void releaseRollbackAndUndeploy() throws Exception {

		// Deploy
		String releaseName = "test2";
		Release release = install("log", "5.0.0", releaseName);
		assertThat(release.getVersion()).isEqualTo(1);

		// Check manifest
		MvcResult result = mockMvc.perform(get("/api/release/manifest/" + releaseName)).andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertThat(result.getResponse().getContentAsString()).isNotEmpty();

		// Upgrade
		String releaseVersion = "2";
		release = upgrade("log", "4.0.0", releaseName);
		assertThat(release.getVersion()).isEqualTo(2);

		// Check manifest
		result = mockMvc.perform(get("/api/release/manifest/" + releaseName + "/2")).andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertThat(result.getResponse().getContentAsString()).isNotEmpty();

		// Rollback to release version 1, creating a third release version equivalent to
		// the 1st.
		releaseVersion = "3";

		Release rollbackRelease = rollback(releaseName, 1);

		release = this.releaseRepository.findByNameAndVersion(releaseName, Integer.valueOf(releaseVersion));
		assertReleaseIsDeployedSuccessfully(releaseName, 3);

		// TODO the common assert doesn't check for this status code.
		assertThat(release.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DEPLOYED);

		// Undeploy
		mockMvc.perform(delete("/api/release/" + releaseName))
				.andDo(print())
				.andExpect(status().isOk()).andReturn();
		Release deletedRelease = this.releaseRepository.findByNameAndVersion(releaseName,
				Integer.valueOf(releaseVersion));
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DELETED);
	}

	@Test
	public void packageDeployAndUpgrade() throws Exception {
		String releaseName = "myLog";
		Release release = install("log", "5.0.0", releaseName);
		assertThat(release.getVersion()).isEqualTo(1);

		// Upgrade
		release = upgrade("log", "4.0.0", releaseName);

		assertThat(release.getVersion()).isEqualTo(2);
	}

	@Test
	public void cancelNonExistingRelease() throws Exception {
		cancel("myLog2", HttpStatus.OK.value(), false);
	}

	@Test
	public void packageDeployAndUpgradeAndCancel() throws Exception {
		String releaseName = "myTestapp";
		Release release = install("testapp", "2.9.0", releaseName);
		assertThat(release.getVersion()).isEqualTo(1);

		// Upgrade
		release = upgrade("testapp", "2.9.1", releaseName, false);
		assertThat(release.getVersion()).isEqualTo(2);

		// Cancel
		cancel(releaseName, HttpStatus.OK.value(), true);
	}

	@Test
	public void testStatusReportsErrorForMissingRelease() throws Exception {
		// In a real container the response is carried over into the error dispatcher, but
		// in the mock a new one is created so we have to assert the status at this
		// intermediate point
		MvcResult result = mockMvc.perform(get("/api/release/status/myLog")).andDo(print())
				.andExpect(status().is4xxClientError()).andReturn();
		MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error"))
				.andReturn();
		assertThat(response.getResponse().getContentAsString()).contains("ReleaseNotFoundException");
	}

	@Test
	public void packageUpgradeWithNoDifference() throws Exception {
		String releaseName = "myPackage";
		String packageName = "log";
		String packageVersion = "5.0.0";
		Release release = install(packageName, packageVersion, releaseName);
		assertThat(release.getVersion()).isEqualTo(1);

		// Upgrade
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		UpgradeProperties upgradeProperties = createUpdateProperties(releaseName);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		PackageMetadata updatePackageMetadata = this.packageMetadataRepository.findByNameAndVersionByMaxRepoOrder(
				packageName,
				packageVersion);
		assertThat(updatePackageMetadata).isNotNull();
		MvcResult result = mockMvc.perform(post("/api/release/upgrade")
				.content(convertObjectToJson(upgradeRequest))).andDo(print())
				.andExpect(status().is4xxClientError()).andReturn();
		assertThat(result.getResolvedException().getMessage()).isEqualTo("Package to upgrade has no difference than existing deployed/deleted package. Not upgrading.");
	}

	@Test
	public void testMutableAttributesAppInstanceStatus() {
		// Test AppStatus with general State set
		AppStatus appStatusWithGeneralState = AppStatus.of("id666").generalState(DeploymentState.deployed).build();
		AppStatus appStatusCopy = DefaultReleaseManager.copyStatus(appStatusWithGeneralState);

		assertThat(appStatusCopy.getState()).isNotNull();
		assertThat(appStatusCopy.getState()).isEqualTo(appStatusWithGeneralState.getState());

		assertThat(appStatusWithGeneralState.getInstances()).isEmpty();
		assertThat(appStatusCopy.getInstances()).isEmpty();

		// Test AppStatus with instances
		AppStatus appStatusWithInstances = AppStatus.of("id666").generalState(null)
				.with(new AppInstanceStatus() {
					@Override
					public String getId() {
						return "instance666";
					}

					@Override
					public DeploymentState getState() {
						return DeploymentState.deployed;
					}

					@Override
					public Map<String, String> getAttributes() {
						return Collections.singletonMap("key1", "value1");
					}
				}).build();

		appStatusCopy = DefaultReleaseManager.copyStatus(appStatusWithInstances);
		appStatusCopy.getInstances().get("instance666").getAttributes().put("key2", "value2");

		assertThat(appStatusWithInstances.getInstances().get("instance666").getAttributes()).hasSize(1);
		assertThat(appStatusCopy.getInstances().get("instance666").getAttributes()).hasSize(2);
		assertThat(appStatusCopy.getInstances().get("instance666").getAttributes()).containsEntry("key2", "value2");

	}

	@Test
	public void getFromAndPostToActuator() throws Exception {
		install("ticktock", "4.0.0", "myTicker");
		assertReleaseIsDeployedSuccessfully("myTicker", 1);

		mockMvc
				.perform(get("/api/release/actuator/myTicker/myTicker.log-v1/myTicker.log-v1-0?endpoint=info"))
				.andExpect(status().isOk()).andReturn();

		verify(actuatorService, times(1))
			.getFromActuator("myTicker", "myTicker.log-v1", "myTicker.log-v1-0","info",
					Optional.empty());


		reset(actuatorService);
		ActuatorPostRequest actuatorPostRequest = ActuatorPostRequest.of("bindings/input",
				Collections.singletonMap("state", "STOPPED"));

		mockMvc
				.perform(post("/api/release/actuator/myTicker/myTicker.log-v1/myTicker.log-v1-0")
						.content(convertObjectToJson(actuatorPostRequest)))
				.andExpect(status().isOk()).andReturn();

		verify(actuatorService, times(1))
				.postToActuator("myTicker", "myTicker.log-v1", "myTicker.log-v1-0",
						actuatorPostRequest, Optional.empty());
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
