/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

import java.net.URI;

import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.web.client.RestClientException;

/**
 * An extension to RestOperations, analogous to postForObject().
 *
 * @author Eric Bottard
 */
interface ExtendedOAuth2RestOperations extends OAuth2RestOperations {

	<T> T putForObject(URI uri, Object request, Class<T> responseType) throws RestClientException;

}
