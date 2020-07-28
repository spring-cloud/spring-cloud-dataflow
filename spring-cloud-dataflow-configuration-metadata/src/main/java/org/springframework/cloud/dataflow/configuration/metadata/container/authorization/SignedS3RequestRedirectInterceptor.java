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

package org.springframework.cloud.dataflow.configuration.metadata.container.authorization;

import java.net.URI;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Redirect interceptor that will remove authorization headers if redirect is tested as an Amazon signed URL.
 *
 * @author Adam J. Weigold
 */
public class SignedS3RequestRedirectInterceptor extends DefaultRedirectStrategy {

    public static final String AMZ_CREDENTIAL = "X-Amz-Credential";

    @Override
    public HttpUriRequest getRedirect(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws ProtocolException {
        final URI uri = getLocationURI(request, response, context);
        final String query = uri.getQuery();
        boolean signedUrl = false;
        if (query != null && query.contains(AMZ_CREDENTIAL)) {
            signedUrl = true;
        }
        final String method = request.getRequestLine().getMethod();
        if (method.equalsIgnoreCase(HttpHead.METHOD_NAME)) {
            HttpHead httpHead = new HttpHead(uri);
            if (!signedUrl) {
                return httpHead;
            } else {
                return new AuthorizationIgnoringHttpRequest(httpHead);
            }
        } else if (method.equalsIgnoreCase(HttpGet.METHOD_NAME)) {
            HttpGet httpGet = new HttpGet(uri);
            if (!signedUrl) {
                return httpGet;
            } else {
                return new AuthorizationIgnoringHttpRequest(httpGet);
            }
        } else {
            final int status = response.getStatusLine().getStatusCode();
            return (status == HttpStatus.SC_TEMPORARY_REDIRECT || status == SC_PERMANENT_REDIRECT)
                    ? RequestBuilder.copy(request).setUri(uri).build()
                    : new HttpGet(uri);
        }
    }

    static class AuthorizationIgnoringHttpRequest extends HttpRequestBase {

        private final HttpRequestBase delegate;

        AuthorizationIgnoringHttpRequest(HttpRequestBase delegate) {
            super();
            this.delegate = delegate;
            setURI(delegate.getURI());
        }

        @Override
        public String getMethod() {
            return delegate.getMethod();
        }

        @Override
        public void addHeader(Header header) {
            if (!header.getName().equalsIgnoreCase("Authorization")) {
                super.addHeader(header);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if (!name.equalsIgnoreCase("Authorization")) {
                super.addHeader(name, value);
            }
        }

        @Override
        public void setHeader(Header header) {
            if (!header.getName().equalsIgnoreCase("Authorization")) {
                super.setHeader(header);
            }
        }

        @Override
        public void setHeader(String name, String value) {
            if (!name.equalsIgnoreCase("Authorization")) {
                super.setHeader(name, value);
            }
        }

        @Override
        public void setHeaders(Header[] headers) {
            Header[] filteredHeaders = Arrays.stream(headers)
                    .filter(header -> !header.getName().equalsIgnoreCase("Authorization"))
                    .toArray(Header[]::new);
            super.setHeaders(filteredHeaders);
        }
    }
}
