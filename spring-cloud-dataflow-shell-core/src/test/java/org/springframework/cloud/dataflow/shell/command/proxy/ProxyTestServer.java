/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.shell.command.proxy;

import java.io.IOException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.extras.SelfSignedSslEngineSource;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

/**
 * Embedded Proxy Server for testing purposes.
 *
 * @author Gunnar Hillert
 */
public class ProxyTestServer {
	public static void main(String[] args) throws IOException {
		final int port = 8077;
		final boolean withSecurity = true;
		final boolean withSsl = true;
		final String proxyServerUsername = "spring";
		final String proxyServerPassword = "cloud";

		HttpProxyServerBootstrap builder = DefaultHttpProxyServer.bootstrap()
			.withFiltersSource(
				new HttpFiltersSourceAdapter() {
					public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
						System.out.println("Request comining in " + originalRequest.uri());
						return new HttpFiltersAdapter(originalRequest) {
							@Override
							public HttpResponse clientToProxyRequest(HttpObject httpObject) {
								return null;
							}

							@Override
							public HttpObject serverToProxyResponse(HttpObject httpObject) {
								return httpObject;
							}
						};
					}
				}
			)
			.withPort(port);

		if (withSecurity) {
			builder.withProxyAuthenticator(new ProxyAuthenticator() {
				@Override
				public String getRealm() {
					return "dataflow";
				}

				@Override
				public boolean authenticate(String userName, String password) {
					if (proxyServerUsername.equals(userName) && proxyServerPassword.equals(password)) {
						return true;
					}
					else {
						return false;
					}
				}
			});
		}

		if (withSsl) {
			builder.withSslEngineSource(new SelfSignedSslEngineSource())
				.withAuthenticateSslClients(false);
		}

		builder.start();
		System.out.println(String.format("Started proxy server on port %s. With security? %s. Ssl enabled? %s", port, withSecurity, withSsl));
		if (withSecurity) {
			System.out.println(String.format("Proxy server username '%s' and password '%s'.", proxyServerUsername, proxyServerPassword));
		}
	}
}
