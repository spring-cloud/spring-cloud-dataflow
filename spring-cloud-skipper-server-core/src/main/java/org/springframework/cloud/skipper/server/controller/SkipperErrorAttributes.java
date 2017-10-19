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
package org.springframework.cloud.skipper.server.controller;

import java.util.Map;

import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.web.context.request.RequestAttributes;

/**
 * Custom {@link ErrorAttributes} adding skipper specific fields for its own
 * errors.
 *
 * @author Janne Valkealahti
 *
 */
public class SkipperErrorAttributes extends DefaultErrorAttributes {

	@Override
	public Map<String, Object> getErrorAttributes(RequestAttributes requestAttributes, boolean includeStackTrace) {
		Map<String, Object> errorAttributes = super.getErrorAttributes(requestAttributes, includeStackTrace);
		Throwable error = getError(requestAttributes);
		if (error != null) {
			// pass in name and version if ReleaseNotFoundException
			if (error instanceof ReleaseNotFoundException) {
				ReleaseNotFoundException e = ((ReleaseNotFoundException) error);
				if (e.getReleaseName() != null) {
					errorAttributes.put("releaseName", e.getReleaseName());
				}
				if (e.getReleaseVersion() != null) {
					errorAttributes.put("version", e.getReleaseVersion());
				}
			}
		}
		return errorAttributes;
	}
}
