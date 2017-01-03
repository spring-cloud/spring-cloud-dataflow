package org.springframework.cloud.dataflow.server.controller;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Vinicius Carvalho
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CompletionControllerTests {

	@Autowired
	private StreamDefinitionRepository repository;

	@Autowired
	private DeploymentIdRepository deploymentIdRepository;

	@Autowired
	private ApplicationConfigurationMetadataResolver metadataResolver;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppDeployer appDeployer;

	@Autowired
	private CommonApplicationProperties appsProperties;

	private final AppRegistry appRegistry = new AppRegistry(
			new InMemoryUriRegistry(),
			new MavenResourceLoader(new MavenProperties())
	);

	@Before
	public void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
		when(appDeployer.deploy(any(AppDeploymentRequest.class))).thenReturn("testID");
	}

	@Test
	public void testMissingArgumentFailure() throws Exception {
		ResultActions result = mockMvc.perform(get("completions/stream").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError());

	}

}
