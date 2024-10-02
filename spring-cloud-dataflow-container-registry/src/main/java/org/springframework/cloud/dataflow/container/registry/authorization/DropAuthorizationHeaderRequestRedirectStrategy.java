/*
 * Copyright 2020-2021 the original author or authors.
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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;

import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.Header;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import org.springframework.util.StringUtils;

/**
 * Amazon, Azure and Custom Container Registry services require special treatment for the Authorization headers when the
 * HTTP request are forwarded to 3rd party services.
 *
 * Amazon:
 *   The Amazon S3 API supports two Authentication Methods (https://amzn.to/2Dg9sga):
 *   (1) HTTP Authorization header and (2) Query string parameters (often referred to as a pre-signed URL).
 *
 *   But only one auth mechanism is allowed at a time. If the http request contains both an Authorization header and
 *   an pre-signed URL parameters then an error is thrown.
 *
 *   Container Registries often use AmazonS3 as a backend object store. If HTTP Authorization header
 *   is used to authenticate with the Container Registry and then this registry redirect the request to a S3 storage
 *   using pre-signed URL authentication, the redirection will fail.
 *
 *   Solution is to implement a HTTP redirect strategy that removes the original Authorization headers when the request is
 *   redirected toward an Amazon signed URL.
 *
 * Azure:
 *   Azure have same type of issues as S3 so header needs to be dropped as well.
 *   (https://docs.microsoft.com/en-us/azure/container-registry/container-registry-faq#authentication-information-is-not-given-in-the-correct-format-on-direct-rest-api-calls)
 *
 * Custom:
 *   Custom Container Registry may have same type of issues as S3 so header needs to be dropped as well.
 *
 * @author Adam J. Weigold
 * @author Janne Valkealahti
 * @author Christian Tzolov
 * @author Cheng Guan Poh
 */
public class DropAuthorizationHeaderRequestRedirectStrategy extends DefaultRedirectStrategy {

	private static final String CUSTOM_REGISTRY = "custom-registry";

	private static final String AMZ_CREDENTIAL = "X-Amz-Credential";

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String AZURECR_URI_SUFFIX = "azurecr.io";

	private static final String BASIC_AUTH = "Basic";

	/**
	 * Additional registry specific configuration properties - usually used inside the Registry authorizer
	 * implementations (eg. the AwsEcrAuthorizer implementation).
	 */
	private Map<String, String> extra;

	public DropAuthorizationHeaderRequestRedirectStrategy(Map<String, String> extra) {
		this.extra = extra;
	}

	@Override
	public URI getLocationURI(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException {

		URI httpUriRequest = super.getLocationURI(request, response, context);
		String query = httpUriRequest.getQuery();
		String method = request.getMethod();

		// Handle Amazon requests
		if (StringUtils.hasText(query) && query.contains(AMZ_CREDENTIAL)) {
			if (isHeadOrGetMethod(method)) {
                try {
                    return new DropAuthorizationHeaderHttpRequestBase(httpUriRequest, method).getUri();
                } catch (URISyntaxException e) {
                    throw new HttpException("Unable to get location URI", e);
                }
            }
		}

		// Handle Azure requests
        try {
            if (request.getUri().getRawPath().contains(AZURECR_URI_SUFFIX)) {
                if (isHeadOrGetMethod(method)) {
                    return (new DropAuthorizationHeaderHttpRequestBase(httpUriRequest, method) {
                        // Drop headers only for the Basic Auth and leave unchanged for OAuth2
                        @Override
                        protected boolean isDropHeader(String name, Object value) {
                            return name.equalsIgnoreCase(AUTHORIZATION_HEADER) && StringUtils.hasText((String) value) && ((String)value).contains(BASIC_AUTH);
                        }
                    }).getUri();
                }
            }


        // Handle Custom requests
		if (extra.containsKey(CUSTOM_REGISTRY) && request.getUri().getRawPath().contains(extra.get(CUSTOM_REGISTRY))) {
			if (isHeadOrGetMethod(method)) {
				return new DropAuthorizationHeaderHttpRequestBase(httpUriRequest, method).getUri();
			}
		}
		} catch (URISyntaxException e) {
			throw new HttpException("Unable to get Locaction URI", e);
		}
		return httpUriRequest;
	}

	private boolean isHeadOrGetMethod(String method) {
		return StringUtils.hasText(method)
				&& (method.equalsIgnoreCase(HttpHead.METHOD_NAME) || method.equalsIgnoreCase(HttpGet.METHOD_NAME));
	}

	/**
	 * Overrides all header setter methods to filter out the Authorization headers.
	 */
	static class DropAuthorizationHeaderHttpRequestBase extends HttpUriRequestBase {

		private final String method;

		DropAuthorizationHeaderHttpRequestBase(URI uri, String method) {
			super(method, uri);
			this.method = method;
		}

		@Override
		public String getMethod() {
			return this.method;
		}

		@Override
		public void addHeader(Header header) {
			if (!isDropHeader(header)) {
				super.addHeader(header);
			}
		}

		@Override
		public void addHeader(String name, Object value) {
			if (!isDropHeader(name, value)) {
				super.addHeader(name, value);
			}
		}

		@Override
		public void setHeader(Header header) {
			if (!isDropHeader(header)) {
				super.setHeader(header);
			}
		}

		@Override
		public void setHeader(String name, Object value) {
			if (!isDropHeader(name, value)) {
				super.setHeader(name, value);
			}
		}

		@Override
		public void setHeaders(Header[] headers) {
			Header[] filteredHeaders = Arrays.stream(headers)
					.filter(header -> !isDropHeader(header))
					.toArray(Header[]::new);
			super.setHeaders(filteredHeaders);
		}

		protected boolean isDropHeader(Header header) {
			return isDropHeader(header.getName(), header.getValue());
		}

		protected boolean isDropHeader(String name, Object value) {
			return name.equalsIgnoreCase(AUTHORIZATION_HEADER);
		}
	}
}
