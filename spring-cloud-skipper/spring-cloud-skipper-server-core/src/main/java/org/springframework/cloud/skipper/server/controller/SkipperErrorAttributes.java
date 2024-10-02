/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.cloud.skipper.server.controller;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.cloud.skipper.PackageDeleteException;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.web.context.request.WebRequest;

/**
 * Custom {@link ErrorAttributes} adding skipper specific fields for its own errors.
 *
 * @author Janne Valkealahti
 *
 */
public class SkipperErrorAttributes extends DefaultErrorAttributes {

	/**
	 * Instantiate a new skipper error attributes. Forces to include error via
	 * override.
	 */
	public SkipperErrorAttributes() {
	}

	@Override
	public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
		Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest,
				options.including(Include.EXCEPTION, Include.MESSAGE).excluding(Include.STACK_TRACE));
		Throwable error = getError(webRequest);
		// if we're in our skipper related exceptions, reset message as available from there
		// as otherwise super method above will resolve message as one possibly set from exception handler
		if (error != null) {
			// pass in name and version if ReleaseNotFoundException
			if (error instanceof ReleaseNotFoundException exception) {
				ReleaseNotFoundException e = exception;
				if (e.getReleaseName() != null) {
					errorAttributes.put("releaseName", e.getReleaseName());
				}
				if (e.getReleaseVersion() != null) {
					errorAttributes.put("version", e.getReleaseVersion());
				}
				errorAttributes.put("message", error.getMessage());
			}
			else if (error instanceof PackageDeleteException) {
				errorAttributes.put("message", error.getMessage());
			}
			else if (error instanceof SkipperException) {
				errorAttributes.put("message", error.getMessage());
			}
		}
		return errorAttributes;
	}
}
