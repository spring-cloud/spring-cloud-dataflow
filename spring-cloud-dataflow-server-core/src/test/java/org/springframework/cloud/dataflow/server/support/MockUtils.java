/*
 * Copyright 2018-2022 the original author or authors.
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
package org.springframework.cloud.dataflow.server.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Deployer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Utility methods to create often used mocks that are more than 1 liners.
 * @author Mark Pollack
 */
public class MockUtils {


	public static SkipperClient configureMock(SkipperClient skipperClient) {
		List<Deployer> deployers = new ArrayList<>();
		deployers.add(createTestDeployer());
		when(skipperClient.listDeployers()).thenReturn(deployers);
		return skipperClient;
	}
	/**
	 * Creates a SkipperClient mock that is configured with a deployer named {@code testPlatform}
	 * @return a SkipperClient mock
	 */
	public static SkipperClient createSkipperClientMock() {
		return configureMock(mock(SkipperClient.class));
	}

	private static Deployer createTestDeployer() {
		return new Deployer("testPlatform", "test", new AppDeployer() {

			@Override
			public String deploy(AppDeploymentRequest request) {
				return null;
			}

			@Override
			public void undeploy(String id) {
			}

			@Override
			public AppStatus status(String id) {
				return null;
			}

			@Override
			public RuntimeEnvironmentInfo environmentInfo() {
				return null;
			}

			@Override
			public String getLog(String id) {
				return null;
			}


		}, mock(ActuatorOperations.class));
	}
}
