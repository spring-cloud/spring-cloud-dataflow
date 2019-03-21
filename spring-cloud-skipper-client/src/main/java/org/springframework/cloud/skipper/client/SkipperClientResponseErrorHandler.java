/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.skipper.PackageDeleteException;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * A {@link ResponseErrorHandler} used in client's RestTemplate to throw
 * various exceptions.
 *
 * @author Janne Valkealahti
 *
 */
public class SkipperClientResponseErrorHandler extends DefaultResponseErrorHandler {

	private ObjectMapper objectMapper;

	/**
	 * Instantiates a new skipper client response error handler.
	 *
	 * @param objectMapper the object mapper
	 */
	public SkipperClientResponseErrorHandler(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "'objectMapper' must be set");
		this.objectMapper = objectMapper;
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		String exceptionClazz = null;
		Map<String, String> map = new HashMap<>();
		try {
			String body = new String(getResponseBody(response));
			@SuppressWarnings("unchecked")
			Map<String, String> parsed = objectMapper.readValue(body, Map.class);
			map.putAll(parsed);
			exceptionClazz = map.get("exception");
		}
		catch (Exception e) {
			// don't want to error here
		}
		if (ObjectUtils.nullSafeEquals(exceptionClazz, ReleaseNotFoundException.class.getName())) {
			handleReleaseNotFoundException(map);
		}
		else if (ObjectUtils.nullSafeEquals(exceptionClazz, PackageDeleteException.class.getName())) {
			handlePackageDeleteException(map);
		}
		else if (ObjectUtils.nullSafeEquals(exceptionClazz, SkipperException.class.getName())) {
			handleSkipperException(map);
		}
		super.handleError(response);
	}


	private void handleReleaseNotFoundException(Map<String, String> map) {
		String releaseName = map.get("releaseName");
		Integer releaseVersion = null;
		if (map.containsKey("releaseVersion")) {
			try {
				releaseVersion = Integer.parseInt(map.get("releaseVersion"));
			}
			catch (Exception e) {
			}
		}
		if (StringUtils.hasText(releaseName)) {
			if (releaseVersion != null) {
				throw new ReleaseNotFoundException(releaseName, releaseVersion);
			}
			else {
				throw new ReleaseNotFoundException(releaseName);
			}
		}
	}

	private void handlePackageDeleteException(Map<String, String> map) {
		String message = map.get("message");
		throw new PackageDeleteException(StringUtils.hasText(message) ? message : "");
	}

	private void handleSkipperException(Map<String, String> map) {
		String message = map.get("message");
		throw new SkipperException(StringUtils.hasText(message) ? message : "");
	}
}
