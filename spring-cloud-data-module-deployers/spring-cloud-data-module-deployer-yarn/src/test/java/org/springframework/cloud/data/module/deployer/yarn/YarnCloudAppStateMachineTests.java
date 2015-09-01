/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.module.deployer.yarn;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.cloud.data.module.deployer.yarn.YarnCloudAppStateMachine.Events;
import org.springframework.cloud.data.module.deployer.yarn.YarnCloudAppStateMachine.States;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

/**
 * Tests for {@link StateMachine} which is controlling
 * module deployment logic with YARN applications.
 *
 * @author Janne Valkealahti
 *
 */
public class YarnCloudAppStateMachineTests {

	@Test
	public void testInitial() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		TestYarnCloudAppService yarnCloudAppService = new TestYarnCloudAppService();
		TaskExecutor taskExecutor = context.getBean(TaskExecutor.class);
		YarnCloudAppStateMachine ycasm = new YarnCloudAppStateMachine(yarnCloudAppService, taskExecutor);
		StateMachine<States, Events> stateMachine = ycasm.buildStateMachine(false);

		StateMachineTestPlan<States, Events> plan =
				StateMachineTestPlanBuilder.<States, Events>builder()
					.defaultAwaitTime(2)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(States.READY)
						.and()
					.build();
		plan.test();
		context.close();
	}

	@Test
	public void testMissingAppVersion() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		TestYarnCloudAppService yarnCloudAppService = new TestYarnCloudAppService();
		TaskExecutor taskExecutor = context.getBean(TaskExecutor.class);
		YarnCloudAppStateMachine ycasm = new YarnCloudAppStateMachine(yarnCloudAppService, taskExecutor);
		StateMachine<States, Events> stateMachine = ycasm.buildStateMachine(false);

		StateMachineTestPlan<States, Events> plan =
				StateMachineTestPlanBuilder.<States, Events>builder()
					.defaultAwaitTime(2)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(States.READY)
						.and()
					.step()
						.sendEvent(Events.DEPLOY)
						.expectStateChanged(3)
						.expectStates(States.ERROR)
						.and()
					.build();
		plan.test();
		context.close();
	}

	@Test
	public void testDeployShouldPushAndStart() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		TestYarnCloudAppService yarnCloudAppService = new TestYarnCloudAppService();
		TaskExecutor taskExecutor = context.getBean(TaskExecutor.class);
		YarnCloudAppStateMachine ycasm = new YarnCloudAppStateMachine(yarnCloudAppService, taskExecutor);
		StateMachine<States, Events> stateMachine = ycasm.buildStateMachine(false);

		Message<Events> message = MessageBuilder.withPayload(Events.DEPLOY)
				.setHeader(YarnCloudAppStateMachine.HEADER_APP_VERSION, "fakeApp")
				.setHeader(YarnCloudAppStateMachine.HEADER_CLUSTER_ID, "fakeClusterId")
				.setHeader(YarnCloudAppStateMachine.HEADER_COUNT, 1)
				.setHeader(YarnCloudAppStateMachine.HEADER_MODULE, "fakeModule")
				.setHeader(YarnCloudAppStateMachine.HEADER_DEFINITION_PARAMETERS, new HashMap<Object, Object>())
				.build();

		StateMachineTestPlan<States, Events> plan =
				StateMachineTestPlanBuilder.<States, Events>builder()
					.defaultAwaitTime(100)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(States.READY)
						.and()
					.step()
						.sendEvent(message)
						.expectStateChanged(8)
						.expectStates(States.READY)
						.and()
					.build();
		plan.test();

		assertThat(yarnCloudAppService.getApplicationsLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.getApplicationsCount, is(1));

		assertThat(yarnCloudAppService.getInstancesLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.getInstancesCount, is(2));

		assertThat(yarnCloudAppService.pushApplicationLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.pushApplicationCount.size(), is(1));
		assertThat(yarnCloudAppService.pushApplicationCount.get(0).appVersion, is("fakeApp"));

		assertThat(yarnCloudAppService.submitApplicationLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.submitApplicationCount.size(), is(1));
		assertThat(yarnCloudAppService.submitApplicationCount.get(0).appVersion, is("fakeApp"));

		assertThat(yarnCloudAppService.createClusterLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.createClusterCount.size(), is(1));
		assertThat(yarnCloudAppService.createClusterCount.get(0).yarnApplicationId, is("fakeApplicationId"));
		assertThat(yarnCloudAppService.createClusterCount.get(0).clusterId, is("fakeClusterId"));
		assertThat(yarnCloudAppService.createClusterCount.get(0).count, is(1));
		assertThat(yarnCloudAppService.createClusterCount.get(0).module, is("fakeModule"));
		assertThat(yarnCloudAppService.createClusterCount.get(0).definitionParameters.size(), is(0));

		assertThat(yarnCloudAppService.startClusterLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.startClusterCount.size(), is(1));
		assertThat(yarnCloudAppService.startClusterCount.get(0).yarnApplicationId, is("fakeApplicationId"));
		assertThat(yarnCloudAppService.startClusterCount.get(0).clusterId, is("fakeClusterId"));

		context.close();
	}

	@Test
	public void testDeployAppAlreadyPushedNotStarted() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		TestYarnCloudAppService yarnCloudAppService = new TestYarnCloudAppService();
		yarnCloudAppService.app = "fakeApp";
		TaskExecutor taskExecutor = context.getBean(TaskExecutor.class);
		YarnCloudAppStateMachine ycasm = new YarnCloudAppStateMachine(yarnCloudAppService, taskExecutor);
		StateMachine<States, Events> stateMachine = ycasm.buildStateMachine(false);

		Message<Events> message = MessageBuilder.withPayload(Events.DEPLOY)
				.setHeader(YarnCloudAppStateMachine.HEADER_APP_VERSION, "fakeApp")
				.setHeader(YarnCloudAppStateMachine.HEADER_CLUSTER_ID, "fakeClusterId")
				.setHeader(YarnCloudAppStateMachine.HEADER_COUNT, 1)
				.setHeader(YarnCloudAppStateMachine.HEADER_MODULE, "fakeModule")
				.setHeader(YarnCloudAppStateMachine.HEADER_DEFINITION_PARAMETERS, new HashMap<Object, Object>())
				.build();

		StateMachineTestPlan<States, Events> plan =
				StateMachineTestPlanBuilder.<States, Events>builder()
					.defaultAwaitTime(2)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(States.READY)
						.and()
					.step()
						.sendEvent(message)
						.expectStateChanged(7)
						.expectStates(States.READY)
						.and()
					.build();
		plan.test();

		assertThat(yarnCloudAppService.getApplicationsLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.getApplicationsCount, is(1));

		assertThat(yarnCloudAppService.getInstancesLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.getInstancesCount, is(2));

		assertThat(yarnCloudAppService.pushApplicationLatch.await(2, TimeUnit.SECONDS), is(false));
		assertThat(yarnCloudAppService.pushApplicationCount.size(), is(0));

		assertThat(yarnCloudAppService.submitApplicationLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.submitApplicationCount.size(), is(1));
		assertThat(yarnCloudAppService.submitApplicationCount.get(0).appVersion, is("fakeApp"));

		assertThat(yarnCloudAppService.createClusterLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.createClusterCount.size(), is(1));
		assertThat(yarnCloudAppService.createClusterCount.get(0).yarnApplicationId, is("fakeApplicationId"));
		assertThat(yarnCloudAppService.createClusterCount.get(0).clusterId, is("fakeClusterId"));
		assertThat(yarnCloudAppService.createClusterCount.get(0).count, is(1));
		assertThat(yarnCloudAppService.createClusterCount.get(0).module, is("fakeModule"));
		assertThat(yarnCloudAppService.createClusterCount.get(0).definitionParameters.size(), is(0));

		assertThat(yarnCloudAppService.startClusterLatch.await(2, TimeUnit.SECONDS), is(true));
		assertThat(yarnCloudAppService.startClusterCount.size(), is(1));
		assertThat(yarnCloudAppService.startClusterCount.get(0).yarnApplicationId, is("fakeApplicationId"));
		assertThat(yarnCloudAppService.startClusterCount.get(0).clusterId, is("fakeClusterId"));

		context.close();
	}

	@Configuration
	static class Config {

		@Bean
		TaskExecutor taskExecutor() {
			ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
			taskExecutor.setCorePoolSize(1);
			return taskExecutor;
		}

	}

	private static class TestYarnCloudAppService implements YarnCloudAppService {

		volatile String app = null;
		volatile String instance = null;

		final CountDownLatch getApplicationsLatch = new CountDownLatch(1);
		final CountDownLatch getInstancesLatch = new CountDownLatch(2);
		final CountDownLatch pushApplicationLatch = new CountDownLatch(1);
		final CountDownLatch submitApplicationLatch = new CountDownLatch(1);
		final CountDownLatch createClusterLatch = new CountDownLatch(1);
		final CountDownLatch startClusterLatch = new CountDownLatch(1);
		final CountDownLatch stopClusterLatch = new CountDownLatch(1);

		volatile int getApplicationsCount = 0;
		volatile int getInstancesCount = 0;

		final List<Wrapper> pushApplicationCount = Collections.synchronizedList(new ArrayList<Wrapper>());
		final List<Wrapper> submitApplicationCount = Collections.synchronizedList(new ArrayList<Wrapper>());
		final List<Wrapper> createClusterCount = Collections.synchronizedList(new ArrayList<Wrapper>());
		final List<Wrapper> startClusterCount = Collections.synchronizedList(new ArrayList<Wrapper>());
		final List<Wrapper> stopClusterCount = Collections.synchronizedList(new ArrayList<Wrapper>());

		@Override
		public Collection<CloudAppInfo> getApplications() {
			ArrayList<CloudAppInfo> infos = new ArrayList<CloudAppInfo>();
			if (app != null) {
				infos.add(new CloudAppInfo(app));
			}
			getApplicationsCount++;
			getApplicationsLatch.countDown();
			return infos;
		}

		@Override
		public Collection<CloudAppInstanceInfo> getInstances() {
			ArrayList<CloudAppInstanceInfo> infos = new ArrayList<CloudAppInstanceInfo>();
			if (instance != null) {
				infos.add(new CloudAppInstanceInfo("fakeApplicationId", instance, "http://fakeAddress"));
			}
			getInstancesCount++;
			getInstancesLatch.countDown();
			return infos;
		}

		@Override
		public void pushApplication(String appVersion) {
			app = appVersion;
			pushApplicationCount.add(new Wrapper(appVersion));
			pushApplicationLatch.countDown();
		}

		@Override
		public String submitApplication(String appVersion) {
			instance = "spring-cloud-data-yarn-app_" + appVersion;
			submitApplicationCount.add(new Wrapper(appVersion));
			submitApplicationLatch.countDown();
			return "fakeApplicationId";
		}

		@Override
		public void createCluster(String yarnApplicationId, String clusterId, int count, String module,
				Map<String, String> definitionParameters) {
			createClusterCount.add(new Wrapper(yarnApplicationId, clusterId, count, module, definitionParameters));
			createClusterLatch.countDown();
		}

		@Override
		public void startCluster(String yarnApplicationId, String clusterId) {
			startClusterCount.add(new Wrapper(yarnApplicationId, clusterId));
			startClusterLatch.countDown();
		}

		@Override
		public void stopCluster(String yarnApplicationId, String clusterId) {
			stopClusterCount.add(new Wrapper(yarnApplicationId, clusterId));
			stopClusterLatch.countDown();
		}

		@Override
		public Map<String, String> getClustersStates() {
			return null;
		}

		@Override
		public Collection<String> getClusters(String yarnApplicationId) {
			return null;
		}

		@Override
		public void destroyCluster(String yarnApplicationId, String clusterId) {
		}

		static class Wrapper {
			String appVersion;
			String yarnApplicationId;
			String clusterId;
			int count;
			String module;
			Map<?, ?> definitionParameters;

			public Wrapper(String appVersion) {
				this.appVersion = appVersion;
			}

			public Wrapper(String yarnApplicationId, String clusterId) {
				this.yarnApplicationId = yarnApplicationId;
				this.clusterId = clusterId;
			}

			public Wrapper(String yarnApplicationId, String clusterId, int count, String module,
					Map<?, ?> definitionParameters) {
				this.yarnApplicationId = yarnApplicationId;
				this.clusterId = clusterId;
				this.count = count;
				this.module = module;
				this.definitionParameters = definitionParameters;
			}

		}
	}

}
