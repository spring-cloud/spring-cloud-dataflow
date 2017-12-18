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
package org.springframework.cloud.dataflow.server.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.support.TestResourceUtils;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.io.DefaultPackageReader;
import org.springframework.cloud.skipper.io.PackageReader;
import org.springframework.cloud.skipper.io.TempFileUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_ENABLED_PROPERTY_KEY;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PACKAGE_NAME;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PACKAGE_VERSION;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@TestPropertySource(properties = { "spring.main.banner-mode=off" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DefaultStreamServiceIntegrationTests {

	@Autowired
	private StreamService streamService;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private AppRegistry appRegistry;

	@MockBean
	private SkipperClient skipperClient;

	@Autowired
	private FeaturesProperties featuresProperties;

	@After
	public void destroyStream() {
		appRegistry.delete("log", ApplicationType.sink);
		appRegistry.delete("time", ApplicationType.source);
		streamService.undeployStream("ticktock");
		streamDefinitionRepository.deleteAll();
	}

	@Test
	public void validateSkipperDeploymentProperites() throws URISyntaxException {
		createTickTock();

		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		// override log to 1.2.0.RELEASE
		deploymentProperties.put("badthing.version.log", "1.2.0.RELEASE");

		try {
			this.featuresProperties.setSkipperEnabled(true);
			streamService.deployStream("ticktock", deploymentProperties);
			fail("Expected an IllegalArgumentException to be thrown.");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Only deployment property keys starting with 'app.' or 'deployer.'  or 'version.' allowed, got 'badthing.version.log'");
		}
	}

	@Test
	public void failSkipperDeploymentWhenSkipperModeDisabled() throws URISyntaxException {
		createTickTock();
		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		try {
			this.featuresProperties.setSkipperEnabled(false);
			streamService.deployStream("ticktock", deploymentProperties);
			fail("Expected an IllegalStateException to be thrown.");
		} catch (IllegalStateException e) {
			assertThat(e.getMessage()).isEqualTo("Skipper mode is not enabled for the Data Flow Server. "
														+ "Try enabling it with the property 'spring.cloud.dataflow.features.skipper-enabled'");
		}
	}

	@Test
	public void testInstallVersionOverride() throws URISyntaxException, IOException {
		createTickTock();

		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		// override log to 1.2.0.RELEASE
		deploymentProperties.put("version.log", "1.2.0.RELEASE");

		this.featuresProperties.setSkipperEnabled(true);
		streamService.deployStream("ticktock", deploymentProperties);

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient).upload(uploadRequestCaptor.capture());

		Package pkg = loadPackageFromBytes(uploadRequestCaptor);

		// ExpectedYaml will have verison: 1.2.0.RELEASE and not 1.1.1.RELEASE
		String expectedYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "install.yml").getInputStream(),
				Charset.defaultCharset());
		Package logPackage = null;
		for (Package subpkg: pkg.getDependencies()) {
			if (subpkg.getMetadata().getName().equals("log")) {
				logPackage = subpkg;
			}
		}
		assertThat(logPackage).isNotNull();
		assertThat(logPackage.getConfigValues().getRaw()).isEqualTo(expectedYaml);
	}

	private Map<String, String> createSkipperDeploymentProperties() {
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put(SKIPPER_ENABLED_PROPERTY_KEY, "true");
		deploymentProperties.put(SKIPPER_PACKAGE_NAME, "ticktock");
		deploymentProperties.put(SKIPPER_PACKAGE_VERSION, "1.0.0");
		return deploymentProperties;
	}

	private void createTickTock() throws URISyntaxException {
		String uri = "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE";
		appRegistry.save("time", ApplicationType.source, new URI(uri),null);
		uri = "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.1.1.RELEASE";
		appRegistry.save("log", ApplicationType.sink, new URI(uri),null);

		// Create stream
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time | log");
		this.streamDefinitionRepository.save(streamDefinition);
	}

	private Package loadPackageFromBytes(ArgumentCaptor<UploadRequest> uploadRequestCaptor) throws IOException {
		PackageReader packageReader = new DefaultPackageReader();
		String packageName = uploadRequestCaptor.getValue().getName();
		String packageVersion= uploadRequestCaptor.getValue().getVersion();
		byte[] packageBytes = uploadRequestCaptor.getValue().getPackageFileAsBytes();
		Path targetPath = TempFileUtils.createTempDirectory("service" + packageName);
		File targetFile = new File(targetPath.toFile(), packageName + "-" + packageVersion + ".zip");
		StreamUtils.copy(packageBytes, new FileOutputStream(targetFile));
		ZipUtil.unpack(targetFile, targetPath.toFile());
		return packageReader
				.read(new File(targetPath.toFile(), packageName + "-" + packageVersion));
	}


}
