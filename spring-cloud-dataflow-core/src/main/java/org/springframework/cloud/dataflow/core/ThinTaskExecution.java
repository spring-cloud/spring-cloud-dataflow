/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.cloud.dataflow.core;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Overrides TaskExecution class to provide CTR status required.
 * @author Corneil du Plessis
 */
public class ThinTaskExecution extends TaskExecution {
    private String ctrTaskStatus;

    public ThinTaskExecution() {
    }
    public ThinTaskExecution(TaskExecution taskExecution) {
        super(taskExecution.getExecutionId(), taskExecution.getExitCode(), taskExecution.getTaskName(), taskExecution.getStartTime(), taskExecution.getEndTime(), taskExecution.getExitMessage(), taskExecution.getArguments(), taskExecution.getErrorMessage(), taskExecution.getExternalExecutionId(), taskExecution.getParentExecutionId());
    }
    public ThinTaskExecution(TaskExecution taskExecution, String ctrTaskStatus) {
        super(taskExecution.getExecutionId(), taskExecution.getExitCode(), taskExecution.getTaskName(), taskExecution.getStartTime(), taskExecution.getEndTime(), taskExecution.getExitMessage(), taskExecution.getArguments(), taskExecution.getErrorMessage(), taskExecution.getExternalExecutionId(), taskExecution.getParentExecutionId());
        this.ctrTaskStatus = ctrTaskStatus;
    }
    public ThinTaskExecution(long executionId, Integer exitCode, String taskName, LocalDateTime startTime, LocalDateTime endTime, String exitMessage, List<String> arguments, String errorMessage, String externalExecutionId, Long parentExecutionId) {
        super(executionId, exitCode, taskName, startTime, endTime, exitMessage, arguments, errorMessage, externalExecutionId, parentExecutionId);
    }

    public ThinTaskExecution(long executionId, Integer exitCode, String taskName, LocalDateTime startTime, LocalDateTime endTime, String exitMessage, List<String> arguments, String errorMessage, String externalExecutionId) {
        super(executionId, exitCode, taskName, startTime, endTime, exitMessage, arguments, errorMessage, externalExecutionId);
    }

    public String getCtrTaskStatus() {
        return ctrTaskStatus;
    }

    public void setCtrTaskStatus(String ctrTaskStatus) {
        this.ctrTaskStatus = ctrTaskStatus;
    }
}
