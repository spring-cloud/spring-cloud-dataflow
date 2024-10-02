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

package org.springframework.cloud.dataflow.shell.command;

import java.util.Map;

import org.springframework.batch.core.JobParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.JobOperations;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionThinResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionProgressInfoResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionResource;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedModel;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;

/**
 * Job commands for the SCDF Shell.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Chris Bono
 * @author Corneil du Plessis
 */
@ShellComponent
public class JobCommands {

	private static final String EXECUTION_DISPLAY = "job execution display";

	private static final String EXECUTION_LIST = "job execution list";

	private static final String EXECUTION_RESTART = "job execution restart";

	private static final String STEP_EXECUTION_LIST = "job execution step list";

	private static final String INSTANCE_DISPLAY = "job instance display";

	private static final String STEP_EXECUTION_PROGRESS = "job execution step progress";

	private static final String STEP_EXECUTION_DISPLAY = "job execution step display";

	@Autowired
	private DataFlowShell dataFlowShell;

	public Availability availableWithViewRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.JOB)
				? Availability.available()
				: Availability.unavailable("you do not have permissions");
	}

	@ShellMethod(key = EXECUTION_LIST, value = "List created job executions filtered by jobName")
	@ShellMethodAvailability("availableWithViewRole")
	public Table executionList(
			@ShellOption(help = "the job name to be used as a filter", defaultValue = ShellOption.NULL) String name) {

		final PagedModel<JobExecutionThinResource> jobs;
		if (name == null) {
			jobs = jobOperations().executionThinList();
		} else {
			jobs = jobOperations().executionThinListByJobName(name);
		}

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();

		modelBuilder.addRow().addValue("ID ").addValue("Task ID").addValue("Job Name ").addValue("Start Time ")
				.addValue("Step Execution Count ").addValue("Definition Status ");
		for (JobExecutionThinResource job : jobs) {
			modelBuilder.addRow().addValue(job.getExecutionId()).addValue(job.getTaskExecutionId())
					.addValue(job.getName())
					.addValue(job.getStartDateTime())
					.addValue(job.getStepExecutionCount())
					.addValue(job.isDefined() ? "Created" : "Destroyed");
		}
		TableBuilder builder = new TableBuilder(modelBuilder.build());

		DataFlowTables.applyStyle(builder);

		return builder.build();
	}

	@ShellMethod(key = EXECUTION_RESTART, value = "Restart a failed job by jobExecutionId")
	@ShellMethodAvailability("availableWithViewRole")
	public String executionRestart(
			@ShellOption(help = "the job executiond id") long id,
			@ShellOption(value = "--useJsonJobParameters",
				help = "boolean value serialize job parameter as Json.  " +
					"Default is null, meaning SCDF default will be used.",
				defaultValue = ShellOption.NULL) String useJsonJobParameters) {
		if(useJsonJobParameters == null) {
			jobOperations().executionRestart(id);
		}
		else {
			jobOperations().executionRestart(id, Boolean.valueOf(useJsonJobParameters));
		}
		return String.format("Restart request has been sent for job execution '%s'", id);
	}

	@ShellMethod(key = EXECUTION_DISPLAY, value = "Display the details of a specific job execution")
	@ShellMethodAvailability("availableWithViewRole")
	public Table executionDisplay(
			@ShellOption(help = "the job execution id") long id) {

		JobExecutionResource jobExecutionResource = jobOperations().jobExecution(id);

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();

		modelBuilder.addRow().addValue("Key ").addValue("Value ");
		modelBuilder.addRow().addValue("Job Execution Id ").addValue(jobExecutionResource.getExecutionId());
		modelBuilder.addRow().addValue("Task Execution Id ").addValue(jobExecutionResource.getTaskExecutionId());
		modelBuilder.addRow().addValue("Task Instance Id ")
				.addValue(jobExecutionResource.getJobExecution().getJobInstance().getInstanceId());
		modelBuilder.addRow().addValue("Job Name ")
				.addValue(jobExecutionResource.getJobExecution().getJobInstance().getJobName());
		modelBuilder.addRow().addValue("Create Time ").addValue(jobExecutionResource.getJobExecution().getCreateTime());
		modelBuilder.addRow().addValue("Start Time ").addValue(jobExecutionResource.getJobExecution().getStartTime());
		modelBuilder.addRow().addValue("End Time ").addValue(jobExecutionResource.getJobExecution().getEndTime());
		modelBuilder.addRow().addValue("Running ").addValue(jobExecutionResource.getJobExecution().isRunning());
		modelBuilder.addRow().addValue("Stopping ").addValue(jobExecutionResource.getJobExecution().isStopping());
		modelBuilder.addRow().addValue("Step Execution Count ")
				.addValue(jobExecutionResource.getJobExecution().getStepExecutions().size());
		modelBuilder.addRow().addValue("Execution Status ")
				.addValue(jobExecutionResource.getJobExecution().getStatus().name());
		modelBuilder.addRow().addValue("Exit Status ")
				.addValue(jobExecutionResource.getJobExecution().getExitStatus().getExitCode());
		modelBuilder.addRow().addValue("Exit Message ")
				.addValue(jobExecutionResource.getJobExecution().getExitStatus().getExitDescription());
		modelBuilder.addRow().addValue("Definition Status ")
				.addValue(jobExecutionResource.isDefined() ? "Created" : "Destroyed");
		modelBuilder.addRow().addValue("Job Parameters ").addValue("");
		for (Map.Entry<String, JobParameter<?>> jobParameterEntry : jobExecutionResource.getJobExecution()
				.getJobParameters().getParameters().entrySet()) {
			String key = org.springframework.util.StringUtils.trimLeadingCharacter(jobParameterEntry.getKey(), '-');
			if (!jobParameterEntry.getValue().isIdentifying()) {
				key = "-" + key;
			}
			String updatedKey = String.format("%s(%s) ", key, jobParameterEntry.getValue().getType().getName());
			modelBuilder.addRow().addValue(updatedKey).addValue(new ArgumentSanitizer().sanitize(key, String.valueOf(jobParameterEntry.getValue())));
		}

		TableBuilder builder = new TableBuilder(modelBuilder.build());

		DataFlowTables.applyStyle(builder);

		return builder.build();
	}

	@ShellMethod(key = INSTANCE_DISPLAY, value = "Display the job executions for a specific job instance.")
	@ShellMethodAvailability("availableWithViewRole")
	public Table instanceDisplay(
			@ShellOption(help = "the job instance id") long id) {

		JobInstanceResource jobInstanceResource = jobOperations().jobInstance(id);

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Name ").addValue("Execution ID ").addValue("Step Execution Count ")
				.addValue("Status ").addValue("Job Parameters ");
		for (JobExecutionResource job : jobInstanceResource.getJobExecutions()) {
			modelBuilder.addRow()
					.addValue(jobInstanceResource.getJobName())
					.addValue(job.getExecutionId())
					.addValue(job.getStepExecutionCount())
					.addValue(job.getJobExecution().getStatus().name())
					.addValue(job.getJobParametersString());
		}
		TableBuilder builder = new TableBuilder(modelBuilder.build());
		DataFlowTables.applyStyle(builder);

		return builder.build();
	}

	@ShellMethod(key = STEP_EXECUTION_LIST, value = "List step executions filtered by jobExecutionId")
	@ShellMethodAvailability("availableWithViewRole")
	public Table stepExecutionList(
			@ShellOption(help = "the job execution id to be used as a filter") long id) {

		final PagedModel<StepExecutionResource> steps = jobOperations().stepExecutionList(id);

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();

		modelBuilder.addRow().addValue("ID ").addValue("Step Name ").addValue("Job Exec Id ").addValue("Start Time ")
				.addValue("End Time ").addValue("Status ");
		for (StepExecutionResource step : steps) {
			modelBuilder.addRow().addValue(step.getStepExecution().getId())
					.addValue(step.getStepExecution().getStepName()).addValue(id)
					.addValue(step.getStepExecution().getStartTime()).addValue(step.getStepExecution().getEndTime())
					.addValue(step.getStepExecution().getStatus().name());
		}
		TableBuilder builder = new TableBuilder(modelBuilder.build());

		DataFlowTables.applyStyle(builder);

		return builder.build();
	}

	@ShellMethod(key = STEP_EXECUTION_PROGRESS, value = "Display the details of a specific step progress")
	@ShellMethodAvailability("availableWithViewRole")
	public Table stepProgressDisplay(
			@ShellOption(help = "the step execution id") long id,
			@ShellOption(value = "--jobExecutionId", help = "the job execution id") long jobExecutionId) {

		StepExecutionProgressInfoResource progressInfoResource = jobOperations().stepExecutionProgress(
				jobExecutionId,
				id);

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("ID ").addValue("Step Name ").addValue("Complete ").addValue("Duration ");

		modelBuilder.addRow().addValue(progressInfoResource.getStepExecution().getId())
				.addValue(progressInfoResource.getStepExecution().getStepName())
				.addValue(progressInfoResource.getPercentageComplete() * 100 + "%")
				.addValue(progressInfoResource.getDuration() + " ms");

		TableBuilder builder = new TableBuilder(modelBuilder.build());
		DataFlowTables.applyStyle(builder);

		return builder.build();
	}

	@ShellMethod(key = STEP_EXECUTION_DISPLAY, value = "Display the details of a specific step execution")
	@ShellMethodAvailability("availableWithViewRole")
	public Table stepExecutionDisplay(
			@ShellOption(help = "the step execution id") long id,
			@ShellOption(value = "--jobExecutionId", help = "the job execution id") long jobExecutionId) {

		StepExecutionProgressInfoResource progressInfoResource = jobOperations().stepExecutionProgress(
				jobExecutionId,
				id);

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Key ").addValue("Value ");
		modelBuilder.addRow().addValue("Step Execution Id ").addValue(id);
		modelBuilder.addRow().addValue("Job Execution Id ").addValue(jobExecutionId);
		modelBuilder.addRow().addValue("Step Name ").addValue(progressInfoResource.getStepExecution().getStepName());
		modelBuilder.addRow().addValue("Start Time ").addValue(progressInfoResource.getStepExecution().getStartTime());
		modelBuilder.addRow().addValue("End Time ").addValue(progressInfoResource.getStepExecution().getEndTime());
		modelBuilder.addRow().addValue("Duration ").addValue(progressInfoResource.getDuration() + " ms");
		modelBuilder.addRow().addValue("Status ").addValue(progressInfoResource.getStepExecution().getStatus().name());
		modelBuilder.addRow().addValue("Last Updated ")
				.addValue(progressInfoResource.getStepExecution().getLastUpdated());
		modelBuilder.addRow().addValue("Read Count ")
				.addValue(progressInfoResource.getStepExecutionHistory().getReadCount().getCount());
		modelBuilder.addRow().addValue("Write Count ")
				.addValue(progressInfoResource.getStepExecutionHistory().getWriteCount().getCount());
		modelBuilder.addRow().addValue("Filter Count ")
				.addValue(progressInfoResource.getStepExecutionHistory().getFilterCount().getCount());
		modelBuilder.addRow().addValue("Read Skip Count ")
				.addValue(progressInfoResource.getStepExecutionHistory().getReadSkipCount().getCount());
		modelBuilder.addRow().addValue("Write Skip Count ")
				.addValue(progressInfoResource.getStepExecutionHistory().getWriteSkipCount().getCount());
		modelBuilder.addRow().addValue("Process Skip Count ")
				.addValue(progressInfoResource.getStepExecutionHistory().getProcessSkipCount().getCount());
		modelBuilder.addRow().addValue("Read Skip Count ")
				.addValue(progressInfoResource.getStepExecutionHistory().getReadSkipCount().getCount());
		modelBuilder.addRow().addValue("Commit Count ")
				.addValue(progressInfoResource.getStepExecutionHistory().getCommitCount().getCount());
		modelBuilder.addRow().addValue("Rollback Count ")
				.addValue(progressInfoResource.getStepExecutionHistory().getRollbackCount().getCount());
		modelBuilder.addRow().addValue("Exit Status ")
				.addValue(progressInfoResource.getStepExecution().getExitStatus().getExitCode());
		modelBuilder.addRow().addValue("Exit Description ")
				.addValue(progressInfoResource.getStepExecution().getExitStatus().getExitDescription());

		TableBuilder builder = new TableBuilder(modelBuilder.build());

		DataFlowTables.applyStyle(builder);

		return builder.build();
	}

	private JobOperations jobOperations() {
		return dataFlowShell.getDataFlowOperations().jobOperations();
	}

}
