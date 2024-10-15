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
package org.springframework.cloud.dataflow.rest.util;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.hc.core5.ssl.SSLContexts;



/**
 * Provides utilities for the Apache {@code HttpClient}, used to make REST calls
 *
 * @author Gunnar Hillert
 */
public class HttpUtils {

	/**
	 * Will create a certificate-ignoring {@link SSLContext}. Please use with utmost
	 * caution as it undermines security, but may be useful in certain testing or
	 * development scenarios.
	 *
	 * @return an SSLContext that will ignore peer certificates
	 */
	public static SSLContext buildCertificateIgnoringSslContext() {
		try {
			return SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build();
		}
		catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new IllegalStateException(
					"Unexpected exception while building the certificate-ignoring SSLContext" + ".", e);
		}
	}
}
