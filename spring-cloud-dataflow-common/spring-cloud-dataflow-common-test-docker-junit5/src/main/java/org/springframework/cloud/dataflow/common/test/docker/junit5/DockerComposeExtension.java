/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.junit5;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.util.AnnotationUtils;

import org.springframework.cloud.dataflow.common.test.docker.compose.DockerComposeRule;
import org.springframework.cloud.dataflow.common.test.docker.junit5.DockerComposeManager.DockerComposeData;

/**
 * {@code JUnit5} extension handling docker compose integration.
 *
 * @author Janne Valkealahti
 *
 */
public class DockerComposeExtension
		implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback, ParameterResolver {

	private static final Namespace NAMESPACE = Namespace.create(DockerComposeExtension.class);

	@Override
	public void beforeAll(ExtensionContext extensionContext) throws Exception {
		// add class level compose info into compose manager
		DockerComposeManager dockerComposeManager = getDockerComposeManager(extensionContext);

		Class<?> testClass = extensionContext.getRequiredTestClass();
		String classKey = extensionContext.getRequiredTestClass().getSimpleName();

		List<DockerCompose> dockerComposeAnnotations = AnnotationUtils.findRepeatableAnnotations(testClass, DockerCompose.class);
		for (DockerCompose dockerComposeAnnotation : dockerComposeAnnotations) {
			DockerComposeData dockerComposeData = new DockerComposeData(dockerComposeAnnotation.id(),
					dockerComposeAnnotation.locations(), dockerComposeAnnotation.services(),
					dockerComposeAnnotation.log(), dockerComposeAnnotation.start(), dockerComposeAnnotation.order());
			dockerComposeManager.addClassDockerComposeData(classKey, dockerComposeData);
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		// add method level compose info into compose manager
		DockerComposeManager dockerComposeManager = getDockerComposeManager(context);

		Method testMethod = context.getRequiredTestMethod();
		String classKey = context.getRequiredTestClass().getSimpleName();
		String methodKey = context.getRequiredTestMethod().getName();

		List<DockerCompose> dockerComposeAnnotations = AnnotationUtils.findRepeatableAnnotations(testMethod, DockerCompose.class);
		for (DockerCompose dockerComposeAnnotation : dockerComposeAnnotations) {
			DockerComposeData dockerComposeData = new DockerComposeData(dockerComposeAnnotation.id(),
					dockerComposeAnnotation.locations(), dockerComposeAnnotation.services(),
					dockerComposeAnnotation.log(), dockerComposeAnnotation.start(), dockerComposeAnnotation.order());
			dockerComposeManager.addMethodDockerComposeData(classKey, methodKey, dockerComposeData);
		}
		dockerComposeManager.build(classKey, methodKey);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		// clean containers related to class and method
		DockerComposeManager dockerComposeManager = getDockerComposeManager(context);
		String classKey = context.getRequiredTestClass().getSimpleName();
		String methodKey = context.getRequiredTestMethod().getName();
		dockerComposeManager.stop(classKey, methodKey);
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return (parameterContext.getParameter().getType() == DockerComposeInfo.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		DockerComposeManager dockerComposeManager = getDockerComposeManager(extensionContext);
		return new DefaultDockerComposeInfo(dockerComposeManager);
	}

	private static DockerComposeManager getDockerComposeManager(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		Store store = getStore(context);
		return store.getOrComputeIfAbsent(testClass, (key)->{return new DockerComposeManager();}, DockerComposeManager.class);
	}

	private static Store getStore(ExtensionContext context) {
		return context.getRoot().getStore(NAMESPACE);
	}

	private static class DefaultDockerComposeInfo implements DockerComposeInfo {
		private final DockerComposeManager dockerComposeManager;

		public DefaultDockerComposeInfo(DockerComposeManager dockerComposeManager) {
			this.dockerComposeManager = dockerComposeManager;
		}

		@Override
		public DockerComposeCluster id(String id) {
			return new DefaultDockerComposeCluster(dockerComposeManager, id);
		}
	}

	private static class DefaultDockerComposeCluster implements DockerComposeCluster {

		private final DockerComposeManager dockerComposeManager;
		private final String id;

		public DefaultDockerComposeCluster(DockerComposeManager dockerComposeManager, String id) {
			this.dockerComposeManager = dockerComposeManager;
			this.id = id;
		}

		@Override
		public DockerComposeRule getRule() {
			return dockerComposeManager.getRule(id);
		}

		@Override
		public void start() {
			dockerComposeManager.startId(id);
		}

		@Override
		public void stop() {
			dockerComposeManager.stopId(id);
		}
	}
}
