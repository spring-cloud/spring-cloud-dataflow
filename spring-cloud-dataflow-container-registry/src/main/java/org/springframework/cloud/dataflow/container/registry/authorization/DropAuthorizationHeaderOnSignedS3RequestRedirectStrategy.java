/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.container.registry.authorization;

import java.net.URI;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * The Amazon S3 API supports two Authentication Methods (https://amzn.to/2Dg9sga):
 * (1) HTTP Authorization header and (2) Query string parameters (often referred to as a pre-signed URL).
 *
 * But only one auth mechanism is allowed at a time. If the http request contains both an Authorization header and
 * an pre-signed URL parameters then an error is thrown.
 *
 * Container Registries often use AmazonS3 as a backend object store. If HTTP Authorization header
 *  is used to authenticate with the Container Registry and then this registry redirect the request to a S3 storage
 *  using pre-signed URL authentication, the redirection will fail.
 *
 * Solution is to implement a HTTP redirect strategy that removes the original Authorization headers when the request is
 * redirected toward an Amazon signed URL.
 *
 * @author Adam J. Weigold
 */
public class DropAuthorizationHeaderOnSignedS3RequestRedirectStrategy extends DefaultRedirectStrategy {

	private static final String AMZ_CREDENTIAL = "X-Amz-Credential";

	private static final String AUTHORIZATION_HEADER = "Authorization";

	@Override
	public HttpUriRequest getRedirect(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws ProtocolException {

		HttpUriRequest httpUriRequest = super.getRedirect(request, response, context);

		final String query = httpUriRequest.getURI().getQuery();

		if (StringUtils.isNoneEmpty(query) && query.contains(AMZ_CREDENTIAL)) {
			final String method = request.getRequestLine().getMethod();
			if (StringUtils.isNoneEmpty(method)
					&& (method.equalsIgnoreCase(HttpHead.METHOD_NAME) || method.equalsIgnoreCase(HttpGet.METHOD_NAME))) {
				return new DropAuthorizationHeaderHttpRequestBase(httpUriRequest.getURI(), method);
			}
		}

		return httpUriRequest;
	}

	/**
	 * Overrides all header setter methods to filter out the Authorization headers.
	 */
	static class DropAuthorizationHeaderHttpRequestBase extends HttpRequestBase {

		private final String method;

		DropAuthorizationHeaderHttpRequestBase(URI uri, String method) {
			super();
			setURI(uri);
			this.method = method;
		}

		@Override
		public String getMethod() {
			return this.method;
		}

		@Override
		public void addHeader(Header header) {
			if (!header.getName().equalsIgnoreCase(AUTHORIZATION_HEADER)) {
				super.addHeader(header);
			}
		}

		@Override
		public void addHeader(String name, String value) {
			if (!name.equalsIgnoreCase(AUTHORIZATION_HEADER)) {
				super.addHeader(name, value);
			}
		}

		@Override
		public void setHeader(Header header) {
			if (!header.getName().equalsIgnoreCase(AUTHORIZATION_HEADER)) {
				super.setHeader(header);
			}
		}

		@Override
		public void setHeader(String name, String value) {
			if (!name.equalsIgnoreCase(AUTHORIZATION_HEADER)) {
				super.setHeader(name, value);
			}
		}

		@Override
		public void setHeaders(Header[] headers) {
			Header[] filteredHeaders = Arrays.stream(headers)
					.filter(header -> !header.getName().equalsIgnoreCase(AUTHORIZATION_HEADER))
					.toArray(Header[]::new);
			super.setHeaders(filteredHeaders);
		}
	}
}
