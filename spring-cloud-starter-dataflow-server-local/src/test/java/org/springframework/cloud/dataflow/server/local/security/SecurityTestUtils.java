/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.dataflow.server.local.security;

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.codec.Base64;
import org.springframework.util.Assert;

/**
 * @author Marius Bogoevici
 * @author Gunnar Hillert
 */
public class SecurityTestUtils {

	/**
	 * Returns a basic authorization header for the given username and password.
	 *
	 * @param username Must not be null
	 * @param password Must not be null
	 * @return Returns the header as String. Never returns null.
	 */
	public static String basicAuthorizationHeader(String username, String password) {
		Assert.notNull(username, "The username must not be null.");
		Assert.notNull(password, "The password must not be null.");

		return "Basic " + new String(Base64.encode((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1)));
	}
}
