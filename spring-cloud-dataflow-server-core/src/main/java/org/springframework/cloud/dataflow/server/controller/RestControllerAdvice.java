/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.admin.service.NoSuchStepExecutionException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.dataflow.server.job.support.JobNotRestartableException;
import org.springframework.cloud.dataflow.server.repository.DuplicateStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.DuplicateTaskException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.hateoas.VndErrors;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Apply common behavior (exception handling etc.,) to all the REST controllers.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@ControllerAdvice
public class RestControllerAdvice {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Handles the general error case. Log track trace at error level
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public VndErrors onException(Exception e) {
		logger.error("Caught exception while handling a request", e);
		String logref = e.getClass().getSimpleName();
		String msg = getExceptionMessage(e);
		return new VndErrors(logref, msg);
	}

	/**
	 * Log the exception message at warn level and stack trace as trace level.
	 * Return response status HttpStatus.CONFLICT
	 */
	@ExceptionHandler({
			AppAlreadyRegisteredException.class,
			DuplicateStreamDefinitionException.class,
			DuplicateTaskException.class,
			StreamAlreadyDeployedException.class,
			StreamAlreadyDeployingException.class})
	@ResponseStatus(HttpStatus.CONFLICT)
	@ResponseBody
	public VndErrors onConflictException(Exception e) {
		String logref = logWarnLevelExceptionMessage(e);
		if (logger.isTraceEnabled()) {
			logTraceLevelStrackTrace(e);
		}
		String msg = getExceptionMessage(e);
		return new VndErrors(logref, msg);
	}

	/**
	 * Log the exception message at warn level and stack trace as trace level.
	 * Return response status HttpStatus.UNPROCESSABLE_ENTITY
	 */
	@ExceptionHandler({JobNotRestartableException.class, JobExecutionNotRunningException.class})
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	@ResponseBody
	public VndErrors onUnprocessableEntityException(Exception e) {
		String logref = logWarnLevelExceptionMessage(e);
		if (logger.isTraceEnabled()) {
			logTraceLevelStrackTrace(e);
		}
		String msg = getExceptionMessage(e);
		return new VndErrors(logref, msg);
	}

	/**
	 * Log the exception message at warn level and stack trace as trace level.
	 * Return response status HttpStatus.NOT_FOUND
	 */
	@ExceptionHandler({NoSuchStreamDefinitionException.class,
			NoSuchAppRegistrationException.class,
			NoSuchTaskDefinitionException.class,
			NoSuchTaskExecutionException.class,
			NoSuchJobExecutionException.class,
			NoSuchJobInstanceException.class,
			NoSuchJobException.class,
			NoSuchStepExecutionException.class,
			MetricsMvcEndpoint.NoSuchMetricException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ResponseBody
	public VndErrors onNotFoundException(Exception e) {
		String logref = logWarnLevelExceptionMessage(e);
		if (logger.isTraceEnabled()) {
			logTraceLevelStrackTrace(e);
		}
		String msg = getExceptionMessage(e);
		return new VndErrors(logref, msg);
	}

	/**
	 * Client did not formulate a correct request.
	 * Log the exception message at warn level and stack trace as trace level.
	 * Return response status HttpStatus.BAD_REQUEST (400).
	 */
	@ExceptionHandler({
		MissingServletRequestParameterException.class,
		MethodArgumentTypeMismatchException.class
	})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public VndErrors onClientGenericBadRequest(Exception e) {
		String logref = logWarnLevelExceptionMessage(e);
		if (logger.isTraceEnabled()) {
			logTraceLevelStrackTrace(e);
		}
		String msg = getExceptionMessage(e);
		return new VndErrors(logref, msg);
	}

	/**
	 * The exception handler is trigger if a JSR303 {@link ConstraintViolationException}
	 * is being raised.
	 *
	 * Log the exception message at warn level and stack trace as trace level.
	 * Return response status HttpStatus.BAD_REQUEST (400).
	 */
	@ExceptionHandler({ConstraintViolationException.class})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public VndErrors onConstraintViolationException(ConstraintViolationException e) {
		String logref = logWarnLevelExceptionMessage(e);
		if (logger.isTraceEnabled()) {
			logTraceLevelStrackTrace(e);
		}

		final StringBuilder errorMessage = new StringBuilder();
		boolean first = true;
		for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
			if (!first) {
				errorMessage.append("; ");
			}
			errorMessage.append(violation.getMessage());
			first = false;
		}

		return new VndErrors(logref, errorMessage.toString());
	}

	private String logWarnLevelExceptionMessage(Exception e) {
		logger.warn("Caught exception while handling a request: " + getExceptionMessage(e));
		return e.getClass().getSimpleName();
	}

	private String logTraceLevelStrackTrace(Throwable t) {
		logger.trace("Caught exception while handling a request", t);
		return t.getClass().getSimpleName();
	}

	private String getExceptionMessage(Exception e) {
		return StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
	}
}
