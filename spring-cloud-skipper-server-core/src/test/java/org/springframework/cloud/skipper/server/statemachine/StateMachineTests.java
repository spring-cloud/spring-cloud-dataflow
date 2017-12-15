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
package org.springframework.cloud.skipper.server.statemachine;

import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.ReleaseDifference;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.deployer.strategies.DeployAppStep;
import org.springframework.cloud.skipper.server.deployer.strategies.HandleHealthCheckStep;
import org.springframework.cloud.skipper.server.deployer.strategies.HealthCheckStep;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.service.PackageService;
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.StateMachineTests.TestConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;

/**
 * Generic tests for skipper statemachine logic. In these tests we simply
 * want to test machine logic meaning we control actions by using
 * mocks for classes actions are using.
 *
 * @author Janne Valkealahti
 *
 */
@SuppressWarnings("unchecked")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext
public class StateMachineTests {

	@Autowired
	private ApplicationContext context;

	@MockBean
	private StateMachineRuntimePersister<SkipperStates, SkipperEvents, String> stateMachineRuntimePersister;

	@MockBean
	private ReleaseManager releaseManager;

	@MockBean
	private PackageService packageService;

	@MockBean
	private ReleaseReportService releaseReportService;

	@MockBean
	private UpgradeStrategy upgradeStrategy;

	@MockBean
	private DeployAppStep deployAppStep;

	@MockBean
	private HealthCheckStep healthCheckStep;

	@MockBean
	private HandleHealthCheckStep handleHealthCheckStep;

	@MockBean
	private ReleaseService releaseService;

	@MockBean
	private ReleaseRepository releaseRepository;

	@SpyBean
	private UpgradeCancelAction upgradeCancelAction;

	@SpyBean
	private ErrorAction errorAction;

	@Test
	public void testFactory() {
		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		assertThat(factory).isNotNull();
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testFactory");
		assertThat(stateMachine).isNotNull();
	}

	@Test
	public void testSimpleInstallShouldNotError() throws Exception {
		Mockito.when(packageService.downloadPackage(any()))
				.thenReturn(new org.springframework.cloud.skipper.domain.Package());
		Mockito.when(releaseService.install(any(), any())).thenReturn(new Release());

		Message<SkipperEvents> message = MessageBuilder
			.withPayload(SkipperEvents.INSTALL)
			.setHeader(SkipperEventHeaders.PACKAGE_METADATA, new PackageMetadata())
			.setHeader(SkipperEventHeaders.INSTALL_PROPERTIES, new InstallProperties())
			.setHeader(SkipperEventHeaders.VERSION, 1)
			.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testInstall");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message)
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(3)
						.expectStateEntered(SkipperStates.INSTALL,
								SkipperStates.INSTALL_INSTALL,
								SkipperStates.INITIAL)
						.and()
					.build();
		plan.test();

		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testSimpleUpgradeShouldNotError() throws Exception {
		Mockito.when(releaseReportService.createReport(any())).thenReturn(new ReleaseAnalysisReport(new ArrayList<>(),
				new ReleaseDifference(true), new Release(), new Release()));
		Mockito.when(upgradeStrategy.checkStatus(any()))
				.thenReturn(true);

		UpgradeRequest upgradeRequest = new UpgradeRequest();

		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE)
				.setHeader(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testSimpleUpgradeShouldNotError");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(9)
						.and()
					.build();
		plan.test();
		Mockito.verify(upgradeCancelAction, never()).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testUpgradeFailsNewAppFailToDeploy() throws Exception {
		Mockito.when(releaseReportService.createReport(any())).thenReturn(new ReleaseAnalysisReport(new ArrayList<>(),
				new ReleaseDifference(true), new Release(), new Release()));
		Mockito.when(upgradeStrategy.checkStatus(any()))
				.thenReturn(false);

		UpgradeRequest upgradeRequest = new UpgradeRequest();

		// timeout 0 for things to fail immediately
		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE)
				.setHeader(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest)
				.setHeader(SkipperEventHeaders.UPGRADE_TIMEOUT, 0L)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testUpgradeFailsNewAppFailToDeploy");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(9)
						.expectStateEntered(SkipperStates.UPGRADE,
								SkipperStates.UPGRADE_START,
								SkipperStates.UPGRADE_DEPLOY_TARGET_APPS,
								SkipperStates.UPGRADE_WAIT_TARGET_APPS,
								SkipperStates.UPGRADE_CHECK_TARGET_APPS,
								SkipperStates.UPGRADE_WAIT_TARGET_APPS,
								SkipperStates.UPGRADE_DEPLOY_TARGET_APPS_FAILED,
								SkipperStates.UPGRADE_CANCEL,
								SkipperStates.INITIAL)
						.and()
					.build();
		plan.test();

		Mockito.verify(upgradeCancelAction).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testUpgradeCancelWhileCheckingApps() throws Exception {
		Mockito.when(releaseReportService.createReport(any())).thenReturn(new ReleaseAnalysisReport(new ArrayList<>(),
				new ReleaseDifference(true), new Release(), new Release()));
		Mockito.when(upgradeStrategy.checkStatus(any()))
				.thenReturn(false);

		UpgradeRequest upgradeRequest = new UpgradeRequest();

		// timeout 60s giving time to try cancel
		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE)
				.setHeader(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest)
				.setHeader(SkipperEventHeaders.UPGRADE_TIMEOUT, 60000L)
				.build();

		Message<SkipperEvents> message2 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE_CANCEL)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testUpgradeCancelWhileCheckingApps");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStateChanged(4)
						.expectStateEntered(SkipperStates.UPGRADE,
								SkipperStates.UPGRADE_START,
								SkipperStates.UPGRADE_DEPLOY_TARGET_APPS,
								SkipperStates.UPGRADE_WAIT_TARGET_APPS)
						.and()
					.step()
						.sendEvent(message2)
						.expectStateChanged(2)
						.expectStateEntered(SkipperStates.UPGRADE_CANCEL,
								SkipperStates.INITIAL)
						.and()
					.build();
		plan.test();

		Mockito.verify(upgradeCancelAction).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testRollbackInstall() throws Exception {
		Release release = new Release();
		Status status = new Status();
		status.setStatusCode(StatusCode.DELETED);
		Info info = Info.createNewInfo("xxx");
		info.setStatus(status);
		release.setInfo(info);
		Mockito.when(releaseRepository.findLatestReleaseForUpdate(any())).thenReturn(release);
		Mockito.when(releaseRepository.findReleaseToRollback(any())).thenReturn(release);
		Mockito.when(releaseService.install(any(Release.class))).thenReturn(release);


		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.ROLLBACK)
				.setHeader(SkipperEventHeaders.RELEASE_NAME, "testRollbackInstall")
				.setHeader(SkipperEventHeaders.ROLLBACK_VERSION, 0)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testRollbackInstall");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(5)
						.expectStateEntered(SkipperStates.ROLLBACK,
								SkipperStates.ROLLBACK_START,
								SkipperStates.INSTALL,
								SkipperStates.INSTALL_INSTALL,
								SkipperStates.INITIAL)
						.and()
					.build();
		plan.test();

		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testInstallDeniedWhileUpgrading() throws Exception {
		Mockito.when(releaseReportService.createReport(any())).thenReturn(new ReleaseAnalysisReport(new ArrayList<>(),
				new ReleaseDifference(true), new Release(), new Release()));
		Mockito.when(upgradeStrategy.checkStatus(any()))
				.thenReturn(false);

		UpgradeRequest upgradeRequest = new UpgradeRequest();

		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE)
				.setHeader(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest)
				.build();

		Message<SkipperEvents> message2 = MessageBuilder
				.withPayload(SkipperEvents.INSTALL)
				.setHeader(SkipperEventHeaders.PACKAGE_METADATA, new PackageMetadata())
				.setHeader(SkipperEventHeaders.INSTALL_PROPERTIES, new InstallProperties())
				.setHeader(SkipperEventHeaders.VERSION, 1)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testInstallDeniedWhileUpgrading");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStateChanged(6)
						.and()
					.build();
		plan.test();

		// install event is not accepted
		boolean accepted = stateMachine.sendEvent(message2);
		assertThat(accepted).isFalse();
	}

	@Import(StateMachineConfiguration.class)
	static class TestConfig {

		@Bean
		public TaskExecutor skipperStateMachineTaskExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(1);
			return executor;
		}

	}
}
