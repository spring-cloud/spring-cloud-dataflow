/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.dataflow.server.batch.NoSuchStepExecutionException;
import org.springframework.cloud.dataflow.server.controller.support.InvalidDateRangeException;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.job.support.JobNotRestartableException;
import org.springframework.cloud.dataflow.server.repository.CannotDeleteNonParentTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.DuplicateStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.DuplicateTaskException;
import org.springframework.cloud.dataflow.server.repository.InvalidApplicationNameException;
import org.springframework.cloud.dataflow.server.repository.NoSuchAuditRecordException;
import org.springframework.cloud.dataflow.server.repository.NoSuchScheduleException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskBatchException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionMissingExternalIdException;
import org.springframework.cloud.dataflow.server.repository.TaskQueryParamException;
import org.springframework.cloud.dataflow.server.service.impl.OffsetOutOfBoundsException;
import org.springframework.cloud.deployer.spi.scheduler.CreateScheduleException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.vnderrors.VndErrors;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
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
 * @author Christian Tzolov
 */
@ControllerAdvice
public class RestControllerAdvice {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Handles the general error case. Log track trace at error level
	 *
	 * @param e the exception not handled by other exception handler methods
	 * @return the error response in JSON format with media type
	 * application/vnd.error+json
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public VndErrors onException(Exception e) {
		logger.error("Caught exception while handling a request", e);
		String logref = e.getClass().getSimpleName();
		String msg = getExceptionMessage(e);
		// TODO: currently need to add these dummy links as deserialization is broken in hateoas
		return new VndErrors(logref, msg, Link.of("/"));
	}

	/**
	 * Log the exception message at warn level and stack trace as trace level. Return
	 * response status HttpStatus.CONFLICT
	 *
	 * @param e one of the exceptions, {@link AppAlreadyRegisteredException},
	 * {@link DuplicateStreamDefinitionException}, {@link DuplicateTaskException},
	 * {@link StreamAlreadyDeployedException}, {@link StreamAlreadyDeployingException},
	 * {@link StreamAlreadyDeployingException}, or {@link ApiNotSupportedException}
	 * @return the error response in JSON format with media type
	 * application/vnd.error+json
	 */
	@ExceptionHandler({
			AppAlreadyRegisteredException.class,
			DuplicateStreamDefinitionException.class,
			DuplicateTaskException.class,
			StreamAlreadyDeployedException.class,
			StreamAlreadyDeployingException.class,
			UnregisterAppException.class,
			InvalidCTRLaunchRequestException.class
	})
	@ResponseStatus(HttpStatus.CONFLICT)
	@ResponseBody
	public VndErrors onConflictException(Exception e) {
		String logref = logWarnLevelExceptionMessage(e);
		if (logger.isTraceEnabled()) {
			logTraceLevelStrackTrace(e);
		}
		String msg = getExceptionMessage(e);
		return new VndErrors(logref, msg, Link.of("/"));
	}

	/**
	 * Log the exception message at warn level and stack trace as trace level. Return
	 * response status HttpStatus.UNPROCESSABLE_ENTITY
	 *
	 * @param e one of the exceptions, {@link JobNotRestartableException} or
	 * {@link JobExecutionNotRunningException}
	 * @return the error response in JSON format with media type
	 * application/vnd.error+json
	 */
	@ExceptionHandler({ JobNotRestartableException.class, JobExecutionNotRunningException.class })
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	@ResponseBody
	public VndErrors onUnprocessableEntityException(Exception e) {
		String logref = logWarnLevelExceptionMessage(e);
		if (logger.isTraceEnabled()) {
			logTraceLevelStrackTrace(e);
		}
		String msg = getExceptionMessage(e);
		return new VndErrors(logref, msg, Link.of("/"));
	}

	/**
	 * Log the exception message at warn level and stack trace as trace level. Return
	 * response status HttpStatus.NOT_FOUND
	 *
	 * @param e one of the exceptions, {@link NoSuchAuditRecordException},
	 * {@link NoSuchStreamDefinitionException},
	 * {@link NoSuchAppRegistrationException}, {@link NoSuchTaskDefinitionException},
	 * {@link NoSuchTaskExecutionException}, {@link NoSuchJobExecutionException},
	 * {@link NoSuchJobInstanceException}, {@link NoSuchJobException},
	 * {@link NoSuchStepExecutionException},
	 * {@link NoSuchAppException},
	 * {@link NoSuchAppInstanceException}, or
	 * @return the error response in JSON format with media type
	 * application/vnd.error+json
	 */
	@ExceptionHandler({ NoSuchAuditRecordException.class,
			NoSuchStreamDefinitionException.class, NoSuchAppRegistrationException.class,
			NoSuchTaskDefinitionException.class, NoSuchTaskExecutionException.class, NoSuchJobExecutionException.class,
			NoSuchJobInstanceException.class, NoSuchJobException.class, NoSuchStepExecutionException.class,
			NoSuchTaskBatchException.class, NoSuchAppException.class, NoSuchAppInstanceException.class,
			NoSuchScheduleException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ResponseBody
	public VndErrors onNotFoundException(Exception e) {
		String logref = logWarnLevelExceptionMessage(e);
		if (logger.isTraceEnabled()) {
			logTraceLevelStrackTrace(e);
		}
		String msg = getExceptionMessage(e);
		return new VndErrors(logref, msg, Link.of("/"));
	}

	/**
	 * Client did not formulate a correct request. Log the exception message at warn level
	 * and stack trace as trace level. Return response status HttpStatus.BAD_REQUEST
	 * (400).
	 *
	 * @param e one of the exceptions, {@link MissingServletRequestParameterException},
	 * {@link UnsatisfiedServletRequestParameterException},
	 * {@link MethodArgumentTypeMismatchException}, {@link InvalidStreamDefinitionException} or
	 * {@link org.springframework.cloud.dataflow.server.repository.InvalidApplicationNameException}
	 * @return the error response in JSON format with media type
	 * application/vnd.error+json
	 */
	@ExceptionHandler({ ApiNotSupportedException.class,MissingServletRequestParameterException.class, HttpMessageNotReadableException.class,
			UnsatisfiedServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
			InvalidDateRangeException.class, CannotDeleteNonParentTaskExecutionException.class,
			InvalidStreamDefinitionException.class, CreateScheduleException.class, OffsetOutOfBoundsException.class,
			TaskExecutionMissingExternalIdException.class, TaskQueryParamException.class, InvalidApplicationNameException.class})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public VndErrors onClientGenericBadRequest(Exception e) {
		String logref = logWarnLevelExceptionMessage(e);
		if (logger.isTraceEnabled()) {
			logTraceLevelStrackTrace(e);
		}

		String message = null;
		if (e instanceof MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
			final Class<?> requiredType = methodArgumentTypeMismatchException.getRequiredType();

			final Class<?> enumType;

			if (requiredType.isEnum()) {
				enumType = requiredType;
			}
			else if (requiredType.isArray() && requiredType.getComponentType().isEnum()) {
				enumType = requiredType.getComponentType();
			}
			else {
				enumType = null;
			}

			if (enumType != null) {
				final String enumValues = StringUtils.arrayToDelimitedString(enumType.getEnumConstants(), ", ");
				message = String.format("The parameter '%s' must contain one of the following values: '%s'.", methodArgumentTypeMismatchException.getName(), enumValues);
			}
		}

		if (message == null) {
			message = getExceptionMessage(e);
		}

		return new VndErrors(logref, message, Link.of("/"));
	}

	/**
	 * The exception handler is trigger if a JSR303 {@link ConstraintViolationException}
	 * is being raised.
	 * <p>
	 * Log the exception message at warn level and stack trace as trace level. Return
	 * response status HttpStatus.BAD_REQUEST (400).
	 *
	 * @param e the exceptions, {@link ConstraintViolationException}
	 * @return the error response in JSON format with media type
	 * application/vnd.error+json
	 */
	@ExceptionHandler({ ConstraintViolationException.class })
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

		return new VndErrors(logref, errorMessage.toString(), Link.of("/"));
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
