/*
 * Copyright 2017-2019 the original author or authors.
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


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.configuration.ComposedRunnerVisitorConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Glenn Renfro
 */
public class ComposedRunnerVisitorTests {

	private static final String CLOSE_CONTEXT_ARG = "--spring.cloud.task.closecontext_enable=false";
	private static final String TASK_NAME_ARG = "--spring.cloud.task.name=job";
	private static final String INVALID_FLOW_MSG = "Invalid flow following '*' specifier.";

	private ConfigurableApplicationContext applicationContext;

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	public void singleTest() {
		setupContextForGraph("AAA");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		assertEquals(1, stepExecutions.size());
		StepExecution stepExecution = stepExecutions.iterator().next();
		assertEquals("AAA_0", stepExecution.getStepName());
	}

	@Test
	public void testFailedGraph() {
		setupContextForGraph("failedStep && AAA");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		assertEquals(1, stepExecutions.size());
		StepExecution stepExecution = stepExecutions.iterator().next();
		assertEquals("failedStep_0", stepExecution.getStepName());
	}

	@Test
	public void testEmbeddedFailedGraph() {
		setupContextForGraph("AAA && failedStep && BBB");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		assertEquals(2, stepExecutions.size());
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertEquals("AAA_0", sortedStepExecution.get(0).getStepName());
		assertEquals("failedStep_0", sortedStepExecution.get(1).getStepName());
	}

//	@Ignore("Disabling till parser can support duplicate tasks")
//	@Test
	public void duplicateTaskTest() {
		setupContextForGraph("AAA && AAA");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		assertEquals(2, stepExecutions.size());
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertEquals("AAA_1", sortedStepExecution.get(0).getStepName());
		assertEquals("AAA_0", sortedStepExecution.get(1).getStepName());

	}

	@Test
	public void testSequential() {
		setupContextForGraph("AAA && BBB && CCC");
		List<StepExecution> stepExecutions = getSortedStepExecutions(getStepExecutions());
		assertEquals(3, stepExecutions.size());
		Iterator<StepExecution> iterator = stepExecutions.iterator();
		StepExecution stepExecution = iterator.next();
		assertEquals("AAA_0", stepExecution.getStepName());
		stepExecution = iterator.next();
		assertEquals("BBB_0", stepExecution.getStepName());
		stepExecution = iterator.next();
		assertEquals("CCC_0", stepExecution.getStepName());
	}

	@Test
	public void splitTest() {
		setupContextForGraph("<AAA||BBB||CCC>");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(3, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("BBB_0"));
		assertTrue(stepNames.contains("CCC_0"));
	}

	@Test
	public void nestedSplit() {
		setupContextForGraph("<<AAA || BBB > && CCC || DDD>", "--splitThreadCorePoolSize=5");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(4, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("BBB_0"));
		assertTrue(stepNames.contains("CCC_0"));
		assertTrue(stepNames.contains("DDD_0"));
	}

	// @Test re-add later
	public void nestedSplitThreadPoolSize() {
		Throwable exception = assertThrows(BeanCreationException.class, () ->
				setupContextForGraph("<<AAA || BBB > && CCC || <DDD || EEE> && FFF>", "--splitThreadCorePoolSize=1"));
		assertThat(exception.getCause().getCause().getMessage()).isEqualTo("Split thread core pool size 1 should be equal or greater than the " +
				"depth of split flows 3. Try setting the composed task property " +
				"`splitThreadCorePoolSize`");
	}

	@Test
	public void twoSplitTest() {
		setupContextForGraph("<AAA||BBB||CCC> && <DDD||EEE>");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(5, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("BBB_0"));
		assertTrue(stepNames.contains("CCC_0"));
		assertTrue(stepNames.contains("DDD_0"));
		assertTrue(stepNames.contains("EEE_0"));
	}

	@Test
	public void testSequentialAndSplit() {
		setupContextForGraph("AAA && <BBB||CCC||DDD> && EEE");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(5, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("BBB_0"));
		assertTrue(stepNames.contains("CCC_0"));
		assertTrue(stepNames.contains("DDD_0"));
		assertTrue(stepNames.contains("EEE_0"));
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertEquals("AAA_0", sortedStepExecution.get(0).getStepName());
		assertEquals("EEE_0", sortedStepExecution.get(4).getStepName());
	}

	@Test
	public void testSequentialTransitionAndSplit() {
		setupContextForGraph("AAA && FFF 'FAILED' -> EEE && <BBB||CCC> && DDD");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(5, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("BBB_0"));
		assertTrue(stepNames.contains("CCC_0"));
		assertTrue(stepNames.contains("DDD_0"));
		assertTrue(stepNames.contains("FFF_0"));
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertEquals("AAA_0", sortedStepExecution.get(0).getStepName());
		assertEquals("DDD_0", sortedStepExecution.get(4).getStepName());
	}

	@Test
	public void testSequentialTransitionAndSplitFailedInvalid() {
		verifyExceptionThrown(INVALID_FLOW_MSG,
				"AAA && failedStep 'FAILED' -> EEE '*' -> FFF && <BBB||CCC> && DDD");
	}

	@Test
	public void testSequentialTransitionAndSplitFailed() {
		setupContextForGraph("AAA && failedStep 'FAILED' -> EEE && FFF && <BBB||CCC> && DDD");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(3, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("failedStep_0"));
		assertTrue(stepNames.contains("EEE_0"));
	}

	@Test
	public void testSequentialAndFailedSplit() {
		setupContextForGraph("AAA && <BBB||failedStep||DDD> && EEE");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(4, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("BBB_0"));
		assertTrue(stepNames.contains("DDD_0"));
		assertTrue(stepNames.contains("failedStep_0"));
	}

	@Test
	public void testSequentialAndSplitWithFlow() {
		setupContextForGraph("AAA && <BBB && FFF||CCC||DDD> && EEE");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(6, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("BBB_0"));
		assertTrue(stepNames.contains("CCC_0"));
		assertTrue(stepNames.contains("DDD_0"));
		assertTrue(stepNames.contains("EEE_0"));
		assertTrue(stepNames.contains("FFF_0"));

		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertEquals("AAA_0", sortedStepExecution.get(0).getStepName());
		assertEquals("EEE_0", sortedStepExecution.get(5).getStepName());
	}

	@Test
	public void testFailedBasicTransition() {
		setupContextForGraph("failedStep 'FAILED' -> AAA * -> BBB");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(2, stepExecutions.size());
		assertTrue(stepNames.contains("failedStep_0"));
		assertTrue(stepNames.contains("AAA_0"));
	}

	@Test
	public void testSuccessBasicTransition() {
		setupContextForGraph("AAA 'FAILED' -> BBB * -> CCC");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(2, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("CCC_0"));
	}

	@Test
	public void testSuccessBasicTransitionWithSequence() {
		verifyExceptionThrown(INVALID_FLOW_MSG,
				"AAA 'FAILED' -> BBB * -> CCC && DDD && EEE");
	}

	@Test
	public void testSuccessBasicTransitionWithTransition() {
		setupContextForGraph("AAA 'FAILED' -> BBB && CCC 'FAILED' -> DDD '*' -> EEE");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(3, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("CCC_0"));
		assertTrue(stepNames.contains("EEE_0"));
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertEquals("AAA_0", sortedStepExecution.get(0).getStepName());
		assertEquals("EEE_0", sortedStepExecution.get(2).getStepName());
	}

	@Test
	public void testSequenceFollowedBySuccessBasicTransitionSequence() {
		verifyExceptionThrown(INVALID_FLOW_MSG,
				"DDD && AAA 'FAILED' -> BBB * -> CCC && EEE");
	}

	@Test
	public void testWildCardOnlyInLastPosition() {
		setupContextForGraph("AAA 'FAILED' -> BBB && CCC * -> DDD ");
		Collection<StepExecution> stepExecutions = getStepExecutions();
		Set<String> stepNames = getStepNames(stepExecutions);
		assertEquals(3, stepExecutions.size());
		assertTrue(stepNames.contains("AAA_0"));
		assertTrue(stepNames.contains("CCC_0"));
		assertTrue(stepNames.contains("DDD_0"));
		List<StepExecution> sortedStepExecution =
				getSortedStepExecutions(stepExecutions);
		assertEquals("AAA_0", sortedStepExecution.get(0).getStepName());
		assertEquals("DDD_0", sortedStepExecution.get(2).getStepName());
	}


	@Test
	public void failedStepTransitionWithDuplicateTaskNameTest() {
		verifyExceptionThrown(
				"Problems found when validating 'failedStep " +
						"'FAILED' -> BBB  && CCC && BBB && EEE': " +
						"[166E:(pos 38): duplicate app name. Use a " +
						"label to ensure uniqueness]",
				"failedStep 'FAILED' -> BBB  && CCC && BBB && EEE");
	}

	@Test
	public void successStepTransitionWithDuplicateTaskNameTest() {
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

	private void setupContextForGraph(String[] args) {
		this.applicationContext = SpringApplication.run(new Class[]{ComposedRunnerVisitorConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				BatchAutoConfiguration.class,
				TaskBatchAutoConfiguration.class,
				SimpleTaskAutoConfiguration.class}, args);
	}

	private Collection<StepExecution> getStepExecutions() {
		JobExplorer jobExplorer = this.applicationContext.getBean(JobExplorer.class);
		List<JobInstance> jobInstances = jobExplorer.findJobInstancesByJobName("job", 0, 1);
		assertEquals(1, jobInstances.size());
		JobInstance jobInstance = jobInstances.get(0);
		List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
		assertEquals(1, jobExecutions.size());
		JobExecution jobExecution = jobExecutions.get(0);
		return jobExecution.getStepExecutions();
	}

	private List<StepExecution> getSortedStepExecutions(Collection<StepExecution> stepExecutions) {
		List<StepExecution> result = new ArrayList<>(stepExecutions);
		result.sort(Comparator.comparing(StepExecution::getStartTime));
		return result;
	}

	private void verifyExceptionThrown(String message, String graph) {
		Throwable exception = assertThrows(BeanCreationException.class, () -> setupContextForGraph(graph));
		assertThat(exception.getCause().getCause().getMessage()).isEqualTo(message);
	}

}
