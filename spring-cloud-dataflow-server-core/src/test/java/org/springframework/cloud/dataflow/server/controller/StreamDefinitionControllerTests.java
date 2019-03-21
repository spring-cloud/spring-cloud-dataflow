/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller;

import java.lang.reflect.Field;
import java.net.URI;

import org.junit.Test;
import sun.misc.Unsafe;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.core.io.ResourceLoader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
 */
public class StreamDefinitionControllerTests {

	// there is nothing to assert in this test other then it does not fail (see GH-1462)
	@Test
	public void validateStreamSaveOutsideOfMVC() throws Exception {

		UriRegistry uriRegistry = mock(UriRegistry.class);
		when(uriRegistry.find("source.foo")).thenReturn(new URI("file://foo"));
		when(uriRegistry.find("sink.bar")).thenReturn(new URI("file://bar"));

		StreamDefinitionController controller = buildStreamDefinitionControllerStub(uriRegistry);
		controller.save("foo", "foo|bar", false);
	}

	private static StreamDefinitionController buildStreamDefinitionControllerStub(UriRegistry uriRegistry)
			throws Exception {
		Unsafe unsafe = getUnsafe();

		StreamDefinitionController controller = (StreamDefinitionController) unsafe
				.allocateInstance(StreamDefinitionController.class);

		AppRegistry appRegistry = new AppRegistry(uriRegistry, mock(ResourceLoader.class));

		DirectFieldAccessor accessor = new DirectFieldAccessor(controller);
		accessor.setPropertyValue("appRegistry", appRegistry);
		accessor.setPropertyValue("repository", mock(StreamDefinitionRepository.class));
		accessor.setPropertyValue("deploymentIdRepository", mock(DeploymentIdRepository.class));

		return controller;
	}

	private static Unsafe getUnsafe() throws Exception {
		Field f = Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Unsafe unsafe = (Unsafe) f.get(null);
		return unsafe;
	}
}
