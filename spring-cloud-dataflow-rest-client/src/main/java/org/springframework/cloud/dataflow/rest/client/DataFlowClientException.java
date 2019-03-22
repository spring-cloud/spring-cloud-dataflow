/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.VndErrors;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A Java exception that wraps the serialized {@link VndErrors} object.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
public class DataFlowClientException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final VndErrors vndErrors;

	/**
	 * Initializes a {@link DataFlowClientException} with the provided (mandatory error).
	 *
	 * @param error Must not be null
	 */
	public DataFlowClientException(VndErrors vndErrors) {
		Assert.notNull(vndErrors, "The provided vndErrors parameter must not be null.");
		this.vndErrors = vndErrors;
	}

	@Override
	public String getMessage() {
		final List<String> errorMessages = new ArrayList<>(0);
		for (VndErrors.VndError e : vndErrors) {
			errorMessages.add(e.getMessage());
		}
		return StringUtils.collectionToDelimitedString(errorMessages, "\n");
	}
}
