/*
 * Copyright 2013-2017 the original author or authors.
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

import java.io.IOException;
import java.util.List;

import org.springframework.hateoas.VndErrors;
import org.springframework.hateoas.VndErrors.VndError;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;

/**
 * Extension of {@link DefaultResponseErrorHandler} that knows how to de-serialize a
 * {@link VndError} structure.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
public class VndErrorResponseErrorHandler extends DefaultResponseErrorHandler {

	private ResponseExtractor<VndErrors> vndErrorsExtractor;

	private ResponseExtractor<VndError> vndErrorExtractor;

	public VndErrorResponseErrorHandler(List<HttpMessageConverter<?>> messageConverters) {
		vndErrorsExtractor = new HttpMessageConverterExtractor<VndErrors>(VndErrors.class, messageConverters);
		vndErrorExtractor = new HttpMessageConverterExtractor<VndError>(VndError.class, messageConverters);
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		VndErrors vndErrors = null;
		try {
			if (HttpStatus.FORBIDDEN.equals(response.getStatusCode())) {
				vndErrors = new VndErrors(vndErrorExtractor.extractData(response));
			}
			else {
				vndErrors = vndErrorsExtractor.extractData(response);
			}
		}
		catch (Exception e) {
			super.handleError(response);
		}
		throw new DataFlowClientException(vndErrors);
	}
}
