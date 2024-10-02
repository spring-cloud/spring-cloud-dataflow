/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DockerComposeFiles;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.ProjectName;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.ShutdownStrategy;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Cluster;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Container;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.ContainerCache;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerMachine;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerPort;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.ClusterHealthCheck;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.ClusterWait;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.HealthCheck;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.ConflictingContainerRemovingDockerCompose;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DefaultDockerCompose;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.Docker;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerCompose;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerComposeExecArgument;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerComposeExecOption;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerComposeExecutable;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerComposeRunArgument;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerComposeRunOption;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerExecutable;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.RetryingDockerCompose;
import org.springframework.cloud.dataflow.common.test.docker.compose.logging.DoNothingLogCollector;
import org.springframework.cloud.dataflow.common.test.docker.compose.logging.FileLogCollector;
import org.springframework.cloud.dataflow.common.test.docker.compose.logging.LogCollector;
import org.springframework.cloud.dataflow.common.test.docker.compose.logging.LogDirectory;

import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.ClusterHealthCheck.serviceHealthCheck;
import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.ClusterHealthCheck.transformingHealthCheck;

public class DockerComposeRule {

	public static final Duration DEFAULT_TIMEOUT = Duration.standardMinutes(2);
	public static final int DEFAULT_RETRY_ATTEMPTS = 2;
	private ProjectName projectName;

	private static final Logger log = LoggerFactory.getLogger(DockerComposeRule.class);

	public DockerPort hostNetworkedPort(int port) {
		return new DockerPort(machine().getIp(), port, port);
	}

	private DockerComposeFiles files;
	private List<ClusterWait> clusterWaits;
	private LogCollector logCollector;
	private DockerMachine machine;
	private boolean pullOnStartup;

	protected DockerComposeRule() {}

	public DockerComposeRule(DockerComposeFiles files, List<ClusterWait> clusterWaits, LogCollector logCollector,
			DockerMachine machine, boolean pullOnStartup, ProjectName projectName) {
		super();
		this.files = files;
		this.clusterWaits = clusterWaits;
		this.logCollector = logCollector;
		this.machine = machine;
		this.pullOnStartup = pullOnStartup;
		this.projectName = projectName != null ? projectName : ProjectName.random();
	}

	public DockerComposeFiles files() {
		return files;
	}

	public List<ClusterWait> clusterWaits() {
		return clusterWaits;
	}

	public DockerMachine machine() {
		return machine != null ? machine : DockerMachine.localMachine().build();
	}

	public ProjectName projectName() {
		return projectName;
	}

	public DockerComposeExecutable dockerComposeExecutable() {
		return DockerComposeExecutable.builder()
			.dockerComposeFiles(files())
			.dockerConfiguration(machine())
			.projectName(projectName())
			.build();
	}

	public DockerExecutable dockerExecutable() {
		return DockerExecutable.builder()
				.dockerConfiguration(machine())
				.build();
	}

	public Docker docker() {
		return new Docker(dockerExecutable());
	}

	public ShutdownStrategy shutdownStrategy() {
		return ShutdownStrategy.KILL_DOWN;
	}

	public DockerCompose dockerCompose() {
		DockerCompose dockerCompose = new DefaultDockerCompose(dockerComposeExecutable(), machine());
		return new RetryingDockerCompose(retryAttempts(), dockerCompose);
	}

	public Cluster containers() {
		return Cluster.builder()
				.ip(machine().getIp())
				.containerCache(new ContainerCache(docker(), dockerCompose()))
				.build();
	}

	protected int retryAttempts() {
		return DEFAULT_RETRY_ATTEMPTS;
	}

	protected boolean removeConflictingContainersOnStartup() {
		return true;
	}

	protected boolean pullOnStartup() {
		return pullOnStartup;
	}

	protected ReadableDuration nativeServiceHealthCheckTimeout() {
		return DEFAULT_TIMEOUT;
	}

	protected LogCollector logCollector() {
		return logCollector != null ? logCollector : new DoNothingLogCollector();
	}

	public void before() throws IOException, InterruptedException {
		log.debug("Starting docker-compose cluster");
		if (pullOnStartup()) {
			dockerCompose().pull();
		}

		dockerCompose().build();

		DockerCompose upDockerCompose = dockerCompose();
		if (removeConflictingContainersOnStartup()) {
			upDockerCompose = new ConflictingContainerRemovingDockerCompose(upDockerCompose, docker());
		}
		upDockerCompose.up();

		logCollector().startCollecting(dockerCompose());
		log.debug("Waiting for services");
		new ClusterWait(ClusterHealthCheck.nativeHealthChecks(), nativeServiceHealthCheckTimeout())
				.waitUntilReady(containers());
		clusterWaits().forEach(clusterWait -> clusterWait.waitUntilReady(containers()));
		log.debug("docker-compose cluster started");
	}

	public void after() {
		try {
			shutdownStrategy().shutdown(this.dockerCompose(), this.docker());
			logCollector().stopCollecting();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error cleaning up docker compose cluster", e);
		}
	}

	public String exec(DockerComposeExecOption options, String containerName,
			DockerComposeExecArgument arguments) throws IOException, InterruptedException {
		return dockerCompose().exec(options, containerName, arguments);
	}

	public String run(DockerComposeRunOption options, String containerName,
			DockerComposeRunArgument arguments) throws IOException, InterruptedException {
		return dockerCompose().run(options, containerName, arguments);
	}

	public static Builder<?> builder() {
		return new Builder<>();
	}

	public static class Builder<T extends Builder<T>> {

		protected DockerComposeFiles files;
		protected List<ClusterWait> clusterWaits = new ArrayList<>();
		protected LogCollector logCollector;
		protected DockerMachine machine;
		protected boolean pullOnStartup;
		protected ProjectName projectName;

		public T files(DockerComposeFiles files) {
			this.files = files;
			return self();
		}

		public T file(String dockerComposeYmlFile) {
			return files(DockerComposeFiles.from(dockerComposeYmlFile));
		}

		/**
		 * Save the output of docker logs to files, stored in the <code>path</code> directory.
		 *
		 * See {@link LogDirectory} for some useful utilities, for example:
		 * {@link LogDirectory#circleAwareLogDirectory}.
		 *
		 * @param path directory into which log files should be saved
		 * @return builder for chaining
		 */
		public T saveLogsTo(String path) {
			return logCollector(FileLogCollector.fromPath(path));
		}

		public T logCollector(LogCollector logCollector) {
			this.logCollector = logCollector;
			return self();
		}

		@Deprecated
		public T waitingForService(String serviceName, HealthCheck<Container> healthCheck) {
			return waitingForService(serviceName, healthCheck, DEFAULT_TIMEOUT);
		}

		public T waitingForService(String serviceName, HealthCheck<Container> healthCheck, ReadableDuration timeout) {
			ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(serviceName, healthCheck);
			return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
		}

		private T addClusterWait(ClusterWait clusterWait) {
			clusterWaits.add(clusterWait);
			return self();
		}

		public T waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck) {
			return waitingForServices(services, healthCheck, DEFAULT_TIMEOUT);
		}

		public T waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck, ReadableDuration timeout) {
			ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(services, healthCheck);
			return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
		}

		public T waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck) {
			return waitingForHostNetworkedPort(port, healthCheck, DEFAULT_TIMEOUT);
		}

		public T waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck, ReadableDuration timeout) {
			ClusterHealthCheck clusterHealthCheck = transformingHealthCheck(cluster -> new DockerPort(cluster.ip(), port, port), healthCheck);
			return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
		}

		public T clusterWaits(Iterable<? extends ClusterWait> elements) {
			elements.forEach(e -> clusterWaits.add(e));
			return self();
		}

		public T machine(DockerMachine machine) {
			this.machine = machine;
			return self();
		}

		public T pullOnStartup(boolean pullOnStartup) {
			this.pullOnStartup = pullOnStartup;
			return self();
		}

		public T projectName(ProjectName projectName) {
			this.projectName = projectName;
			return self();
		}

		@SuppressWarnings("unchecked")
		final T self() {
			return (T) this;
		}

		public DockerComposeRule build() {
			return new DockerComposeRule(files, clusterWaits, logCollector, machine, pullOnStartup, projectName);
		}
	}
}
