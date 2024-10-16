/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.configuration.ComposedRunnerVisitorConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
public class ComposedRunnerVisitorTests {

	private static final String CLOSE_CONTEXT_ARG = "--spring.cloud.task.closecontext_enable=false";
	private static final String TASK_NAME_ARG = "--spring.cloud.task.name=job";
	private static final String INVALID_FLOW_MSG = "Invalid flow following '*' specifier.";

	private ConfigurableApplicationContext applicationContext;

	@AfterEach
	void tearDown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	void singleTest() {
		setupContextForGraph("AAA");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		assertThat(stepExecutions).hasSize(1);
		StepExecution stepExecution = stepExecutions.iterator().next();
		assertThat(stepExecution.getStepName()).isEqualTo("AAA_0");
	}

	@Test
	void singleTestForuuIDIncrementer() {
		setupContextForGraph("AAA", "--uuIdInstanceEnabled=true");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		assertThat(stepExecutions).hasSize(1);
		StepExecution stepExecution = stepExecutions.iterator().next();
		assertThat(stepExecution.getStepName()).isEqualTo("AAA_0");
	}

	@Test
	void failedGraph() {
		setupContextForGraph("failedStep && AAA");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		assertThat(stepExecutions).hasSize(1);
		StepExecution stepExecution = stepExecutions.iterator().next();
		assertThat(stepExecution.getStepName()).isEqualTo("failedStep_0");
	}

	@Test
	void embeddedFailedGraph() {
		setupContextForGraph("AAA && failedStep && BBB");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		assertThat(stepExecutions).hasSize(2);
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertThat(sortedStepExecution.get(0).getStepName()).isEqualTo("AAA_0");
		assertThat(sortedStepExecution.get(1).getStepName()).isEqualTo("failedStep_0");
	}

//	@Ignore("Disabling till parser can support duplicate tasks")
//	@Test
	public void duplicateTaskTest() {
		setupContextForGraph("AAA && AAA");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		assertThat(stepExecutions).hasSize(2);
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertThat(sortedStepExecution.get(0).getStepName()).isEqualTo("AAA_1");
		assertThat(sortedStepExecution.get(1).getStepName()).isEqualTo("AAA_0");

	}

	@Test
	void sequential() {
		setupContextForGraph("AAA && BBB && CCC");
		List<StepExecution> stepExecutions = getSortedStepExecutions(getStepExecutions());
		assertThat(stepExecutions).hasSize(3);
		Iterator<StepExecution> iterator = stepExecutions.iterator();
		StepExecution stepExecution = iterator.next();
		assertThat(stepExecution.getStepName()).isEqualTo("AAA_0");
		stepExecution = iterator.next();
		assertThat(stepExecution.getStepName()).isEqualTo("BBB_0");
		stepExecution = iterator.next();
		assertThat(stepExecution.getStepName()).isEqualTo("CCC_0");
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void splitTest(int threadCorePoolSize) {
		setupContextForGraph("<AAA||BBB||CCC>", "--splitThreadCorePoolSize=" + threadCorePoolSize);
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(3);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("BBB_0");
		assertThat(stepNames).contains("CCC_0");
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 5})
	void nestedSplit(int threadCorePoolSize) {
		setupContextForGraph("<<AAA || BBB > && CCC || DDD>", "--splitThreadCorePoolSize=" + threadCorePoolSize);
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(4);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("BBB_0");
		assertThat(stepNames).contains("CCC_0");
		assertThat(stepNames).contains("DDD_0");
	}

	@Test
	void nestedSplitThreadPoolSize() {
		assertThatThrownBy(() ->
				setupContextForGraph("<<AAA || BBB > && CCC || <DDD || EEE> && FFF>", "--splitThreadCorePoolSize=2")
		).hasCauseInstanceOf(BeanCreationException.class)
				.hasRootCauseMessage("Split thread core pool size 2 should be equal or greater than the " +
				"depth of split flows 3. Try setting the composed task property " +
				"`splitThreadCorePoolSize`");
	}

	@Test
	void sequentialNestedSplitThreadPoolSize() {
		setupContextForGraph("<<AAA || BBB> || <CCC || DDD>> && <EEE || FFF>", "--splitThreadCorePoolSize=3");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(6);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("BBB_0");
		assertThat(stepNames).contains("CCC_0");
		assertThat(stepNames).contains("DDD_0");
		assertThat(stepNames).contains("EEE_0");
		assertThat(stepNames).contains("FFF_0");
	}


	@Test
	void twoSplitTest() {
		setupContextForGraph("<AAA||BBB||CCC> && <DDD||EEE>");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(5);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("BBB_0");
		assertThat(stepNames).contains("CCC_0");
		assertThat(stepNames).contains("DDD_0");
		assertThat(stepNames).contains("EEE_0");
	}

	@Test
	void sequentialAndSplit() {
		setupContextForGraph("AAA && <BBB||CCC||DDD> && EEE");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(5);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("BBB_0");
		assertThat(stepNames).contains("CCC_0");
		assertThat(stepNames).contains("DDD_0");
		assertThat(stepNames).contains("EEE_0");
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertThat(sortedStepExecution.get(0).getStepName()).isEqualTo("AAA_0");
		assertThat(sortedStepExecution.get(4).getStepName()).isEqualTo("EEE_0");
	}

	@Test
	void sequentialTransitionAndSplit() {
		setupContextForGraph("AAA && FFF 'FAILED' -> EEE && <BBB||CCC> && DDD");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(5);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("BBB_0");
		assertThat(stepNames).contains("CCC_0");
		assertThat(stepNames).contains("DDD_0");
		assertThat(stepNames).contains("FFF_0");
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertThat(sortedStepExecution.get(0).getStepName()).isEqualTo("AAA_0");
		assertThat(sortedStepExecution.get(4).getStepName()).isEqualTo("DDD_0");
	}

	@Test
	void sequentialTransitionAndSplitFailedInvalid() {
		verifyExceptionThrown(INVALID_FLOW_MSG,
				"AAA && failedStep 'FAILED' -> EEE '*' -> FFF && <BBB||CCC> && DDD");
	}

	@Test
	void sequentialTransitionAndSplitFailed() {
		setupContextForGraph("AAA && failedStep 'FAILED' -> EEE && FFF && <BBB||CCC> && DDD");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(3);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("failedStep_0");
		assertThat(stepNames).contains("EEE_0");
	}

	@Test
	void sequentialAndFailedSplit() {
		setupContextForGraph("AAA && <BBB||failedStep||DDD> && EEE");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(4);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("BBB_0");
		assertThat(stepNames).contains("DDD_0");
		assertThat(stepNames).contains("failedStep_0");
	}

	@Test
	void sequentialAndSplitWithFlow() {
		setupContextForGraph("AAA && <BBB && FFF||CCC||DDD> && EEE");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(6);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("BBB_0");
		assertThat(stepNames).contains("CCC_0");
		assertThat(stepNames).contains("DDD_0");
		assertThat(stepNames).contains("EEE_0");
		assertThat(stepNames).contains("FFF_0");

		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertThat(sortedStepExecution.get(0).getStepName()).isEqualTo("AAA_0");
		assertThat(sortedStepExecution.get(5).getStepName()).isEqualTo("EEE_0");
	}

	@Test
	void failedBasicTransition() {
		setupContextForGraph("failedStep 'FAILED' -> AAA * -> BBB");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(2);
		assertThat(stepNames).contains("failedStep_0");
		assertThat(stepNames).contains("AAA_0");
	}

	@Test
	void successBasicTransition() {
		setupContextForGraph("AAA 'FAILED' -> BBB * -> CCC");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(2);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("CCC_0");
	}

	@Test
	void successBasicTransitionWithSequence() {
		verifyExceptionThrown(INVALID_FLOW_MSG,
				"AAA 'FAILED' -> BBB * -> CCC && DDD && EEE");
	}

	@Test
	void successBasicTransitionWithTransition() {
		setupContextForGraph("AAA 'FAILED' -> BBB && CCC 'FAILED' -> DDD '*' -> EEE");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(3);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("CCC_0");
		assertThat(stepNames).contains("EEE_0");
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertThat(sortedStepExecution.get(0).getStepName()).isEqualTo("AAA_0");
		assertThat(sortedStepExecution.get(2).getStepName()).isEqualTo("EEE_0");
	}

	@Test
	void sequenceFollowedBySuccessBasicTransitionSequence() {
		verifyExceptionThrown(INVALID_FLOW_MSG,
				"DDD && AAA 'FAILED' -> BBB * -> CCC && EEE");
	}

	@Test
	void wildCardOnlyInLastPosition() {
		setupContextForGraph("AAA 'FAILED' -> BBB && CCC * -> DDD ");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertThat(stepExecutions).hasSize(3);
		assertThat(stepNames).contains("AAA_0");
		assertThat(stepNames).contains("CCC_0");
		assertThat(stepNames).contains("DDD_0");
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertThat(sortedStepExecution.get(0).getStepName()).isEqualTo("AAA_0");
		assertThat(sortedStepExecution.get(2).getStepName()).isEqualTo("DDD_0");
	}


	@Test
	void failedStepTransitionWithDuplicateTaskNameTest() {
		verifyExceptionThrown(
				"Problems found when validating 'failedStep " +
						"'FAILED' -> BBB  && CCC && BBB && EEE': " +
						"[166E:(pos 38): duplicate app name. Use a " +
						"label to ensure uniqueness]",
				"failedStep 'FAILED' -> BBB  && CCC && BBB && EEE");
	}

	@Test
	void successStepTransitionWithDuplicateTaskNameTest() {
		verifyExceptionThrown(
				"Problems found when validating 'AAA 'FAILED' -> " +
						"BBB  * -> CCC && BBB && EEE': [166E:(pos 33): " +
						"duplicate app name. Use a label to ensure " +
						"uniqueness]", "AAA 'FAILED' -> BBB  * -> CCC && BBB && EEE");
	}


	private Set<String> getStepNames(Collection<StepExecution> stepExecutions) {
		Set<String> result = new HashSet<>();
		for (StepExecution stepExecution : stepExecutions) {
			result.add(stepExecution.getStepName());
		}
		return result;
	}

	private void setupContextForGraph(String graph, String... args) {
		List<String> argsForCtx = new ArrayList<>(Arrays.asList(args));
		argsForCtx.add("--graph=" + graph);
		argsForCtx.add(CLOSE_CONTEXT_ARG);
		argsForCtx.add(TASK_NAME_ARG);
		argsForCtx.add("--spring.batch.initialize-schema=always");
		argsForCtx.add("--spring.main.web-application-type=none");
		setupContextForGraph(argsForCtx.toArray(new String[0]));
	}

	private void setupContextForGraph(String[] args) throws RuntimeException{
		this.applicationContext = SpringApplication.
			run(new Class[]{ ComposedRunnerVisitorTestsConfiguration.class,
				ComposedRunnerVisitorConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class,
				SimpleTaskAutoConfiguration.class}, args);
	}


	private Collection<StepExecution> getStepExecutions() {
		JobExplorer jobExplorer = this.applicationContext.getBean(JobExplorer.class);
		List<JobInstance> jobInstances = jobExplorer.findJobInstancesByJobName("job", 0, 1);
		assertThat(jobInstances).hasSize(1);
		JobInstance jobInstance = jobInstances.get(0);
		List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
		assertThat(jobExecutions).hasSize(1);
		JobExecution jobExecution = jobExecutions.get(0);
		assertThat(jobExecution.getJobParameters().getParameters().get("ctr.id")).isNotNull();
		return jobExecution.getStepExecutions();
	}

	private List<StepExecution> getSortedStepExecutions(Collection<StepExecution> stepExecutions) {
		List<StepExecution> result = new ArrayList<>(stepExecutions);
		result.sort(Comparator.comparing(StepExecution::getStartTime));
		return result;
	}

	private void verifyExceptionThrown(String message, String graph) {
		assertThatThrownBy(() -> setupContextForGraph(graph))
				.hasRootCauseMessage(message);
	}

	@Configuration
	public static class ComposedRunnerVisitorTestsConfiguration  {
		@Autowired
		DataSource dataSource;
		@Bean
		public PlatformTransactionManager transactionManager() {
			return new JdbcTransactionManager(dataSource);
		}
	}

}
