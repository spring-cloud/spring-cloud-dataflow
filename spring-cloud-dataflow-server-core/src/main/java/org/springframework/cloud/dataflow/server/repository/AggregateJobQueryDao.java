package org.springframework.cloud.dataflow.server.repository;



import java.util.Date;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecutions;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AggregateJobQueryDao {
	Page<JobInstanceExecutions> listJobInstances(String jobName, Pageable pageable);

	Page<TaskJobExecution> listJobExecutions(String jobName, BatchStatus status, Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsBetween(Date fromDate, Date toDate, Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsWithSteps(Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsWithStepCount(Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(int jobInstanceId, String schemaTarget, Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(int taskExecutionId, String schemaTarget, Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsForJobWithStepCount(String jobName, Pageable pageable);

	JobInstanceExecutions getJobInstanceExecution(String jobName, long instanceId);
}
