package org.springframework.cloud.dataflow.module.deployer.kubernetes;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.fabric8.kubernetes.client.KubernetesClient;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = KubernetesModuleDeployerConfiguration.class)
public class KubernetesModuleDeployerIT {

	@Autowired
	private ModuleDeployer processModuleDeployer;

	@Autowired
	private KubernetesClient kubernetesClient;

	private ExecutorService executor;

	@Before
	public void setup() {
		executor = Executors.newSingleThreadExecutor();
	}

	@After
	public void teardown() {
		executor.shutdownNow();
	}

	@Test
	public void end2endDeployment1() throws InterruptedException, ExecutionException  {

		String group = "deployment-test-0";
		String name = "http";

		ModuleDefinition d = new ModuleDefinition.Builder()
				.setName("foobar")
				.setGroup(group)
				.setLabel(name)
				.setParameter("server.port", "9999")
				.build();

		ArtifactCoordinates c = new ArtifactCoordinates.Builder()
				.setGroupId("org.springframework.cloud.stream.module")
				.setArtifactId("http-source")
				.setExtension("jar")
				.setVersion("1.0.0.BUILD-SNAPSHOT")
				.build();
								
		ModuleDeploymentRequest request = new ModuleDeploymentRequest(d, c);
		
		final ModuleDeploymentId id = processModuleDeployer.deploy(request);
		assertNotNull(id);
		
		ModuleStatus status = processModuleDeployer.status(id);
		assertNotNull(status);
		assertSame(id, status.getModuleDeploymentId());

		// wait for "deploying" state. 
		final Future<ModuleStatus.State> future = executor.submit(new Callable<ModuleStatus.State>() {
		    @Override
		    public ModuleStatus.State call() throws Exception {
		    	while ( processModuleDeployer.status(id).getState() != ModuleStatus.State.deploying ) {
					Thread.sleep(2000);
				}		    	
		    	return processModuleDeployer.status(id).getState();

		    }
		});
		try {
			assertEquals(ModuleStatus.State.deploying, future.get(15000, TimeUnit.MILLISECONDS));
		} catch (TimeoutException e) {
		    future.cancel(true);
		    fail(e.getMessage());
		}

		processModuleDeployer.undeploy(id);

		// ensure the corresponding Kubernetes entities have been undeployed
		Map<String, String> labels = createLabelMap(group, name);
		assertTrue(kubernetesClient.services().withLabels(labels).list().getItems().isEmpty());
		assertTrue(kubernetesClient.replicationControllers().withLabels(labels).list().getItems().isEmpty());
		assertTrue(kubernetesClient.pods().withLabels(labels).list().getItems().isEmpty());
	}

	private Map<String, String> createLabelMap(String group, String label) {
		Map<String, String> map = new HashMap<>();
		map.put(KubernetesModuleDeployer.SCSM_GROUP_KEY, group);
		map.put(KubernetesModuleDeployer.SCSM_LABEL_KEY, label);
		return map;
	}

}
