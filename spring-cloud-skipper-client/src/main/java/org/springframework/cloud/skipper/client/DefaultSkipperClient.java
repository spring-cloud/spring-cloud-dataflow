/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.AboutInfo;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * The default implementation to communicate with the Skipper Server.
 *
 * @author Mark Pollack
 */
public class DefaultSkipperClient implements SkipperClient {

	private static final Logger log = LoggerFactory.getLogger(DefaultSkipperClient.class);

	protected final RestTemplate restTemplate;

	private final String baseUrl;

	/**
	 * Create a new DefaultSkipperClient given the URL of the Server.  This constructor
	 * will create a new RestTemplate instance for communication.
	 *
	 * @param baseUrl the URL of the Server.
	 */
	public DefaultSkipperClient(String baseUrl) {
		Assert.notNull(baseUrl, "The provided baseUrl must not be null.");
		this.baseUrl = baseUrl;
		this.restTemplate = new RestTemplate();
	}

	/**
	 * Create a new DefaultSkipperClient given the URL of the Server and a preconfigured
	 * RestTemplate.
	 *
	 * @param baseUrl      the URL of the Server.
	 * @param restTemplate the template to use to make http calls to the server.
	 */
	public DefaultSkipperClient(String baseUrl, RestTemplate restTemplate) {
		Assert.notNull(baseUrl, "The provided baseURI must not be null.");
		Assert.notNull(restTemplate, "The provided restTemplate must not be null.");
		this.baseUrl = baseUrl;
		this.restTemplate = restTemplate;
	}

	@Override
	public AboutInfo getAboutInfo() {
		// TODO use Traverson API to find 'about' resource.
		return restTemplate.getForObject(baseUrl + "/about", AboutInfo.class);
	}
}
