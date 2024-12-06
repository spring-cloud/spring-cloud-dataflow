package org.springframework.cloud.dataflow.unit.test;

import java.util.concurrent.atomic.AtomicBoolean;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.server.single.DataFlowServerApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("keycloak")
@SpringBootTest(classes = { DataFlowServerApplication.class },
		webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@Disabled("Determine how to run app and test client in different contexts")
public class DataFlowAuthenticationTests {

	private static final Logger logger = LoggerFactory.getLogger(DataFlowAuthenticationTests.class);

	@Container
	static KeycloakContainer keycloakContainer = new KeycloakContainer("keycloak/keycloak:25.0")
		.withRealmImportFiles("/dataflow-realm.json", "/dataflow-users-0.json")
		.withAdminUsername("admin")
		.withAdminPassword("admin")
		.withExposedPorts(8080, 9000)
		.withLogConsumer(outputFrame -> {
			switch (outputFrame.getType()) {
				case STDERR:
					logger.error(outputFrame.getUtf8StringWithoutLineEnding());
					break;
				default:
					logger.info(outputFrame.getUtf8StringWithoutLineEnding());
			}
		});

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("keycloak.url", keycloakContainer::getAuthServerUrl);
	}

	@Test
	void testAuthentication() throws Exception {
		try (ConfigurableApplicationContext applicationContext = SpringApplication.run(CommandLineApp.class,
				"--spring.profiles.active=keycloak-client",
				"--spring.cloud.dataflow.client.authentication.basic.username=joe",
				"--spring.cloud.dataflow.client.authentication.basic.password=password",
				"--keycloak.url=" + keycloakContainer.getAuthServerUrl(),
				"--spring.cloud.dataflow.client.authentication.token-uri=" + keycloakContainer.getAuthServerUrl()
						+ "/realms/dataflow/protocol/openid-connect/token")) {
			DataFlowOperations dataFlowOperations = applicationContext.getBean(DataFlowOperations.class);
			assertThat(dataFlowOperations).isNotNull();
			AboutResource aboutResource = dataFlowOperations.aboutOperation().get();
			assertThat(aboutResource).isNotNull();
			assertThat(aboutResource.getSecurityInfo()).isNotNull();
			assertThat(aboutResource.getSecurityInfo().isAuthenticated()).isTrue();
			assertThat(aboutResource.getSecurityInfo().getUsername()).isEqualTo("joe");
			CommandLineApp.completed.set(true);
		}
		finally {
			CommandLineApp.completed.set(true);
		}
	}

	@SpringBootApplication
	@ImportAutoConfiguration(DataFlowClientAutoConfiguration.class)
	public static class CommandLineApp implements CommandLineRunner {

		public static AtomicBoolean completed = new AtomicBoolean(false);

		@Override
		public void run(String... args) throws Exception {
			Awaitility.await().until(() -> completed.get());
		}

	}

}
