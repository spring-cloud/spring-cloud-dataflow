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
package org.springframework.cloud.skipper.shell.command.support;

import org.springframework.cloud.skipper.client.DefaultSkipperClient;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.client.util.HttpClientConfigurer;
import org.springframework.cloud.skipper.client.util.ProcessOutputResource;
import org.springframework.cloud.skipper.client.util.ResourceBasedAuthorizationInterceptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * A singleton object that can be passed around while changing the target instance.
 *
 * @author Mark Pollack
 */
@Component
public class TargetHolder implements ApplicationEventPublisherAware {

	private Target target;

	private final RestTemplate restTemplate;

	private ApplicationEventPublisher applicationEventPublisher;

	private String credentialsProviderCommand;

	public TargetHolder(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * Return the {@link Target} which encapsulates not only the Target URI but also
	 * success/error messages + status.
	 */
	public Target getTarget() {
		return target;
	}

	/**
	 * Set the Skipper Server {@link Target}.
	 *
	 * @param target Must not be null.
	 * @param credentialsProviderCommand
	 */
	public void changeTarget(Target target, String credentialsProviderCommand) throws Exception {
		Assert.notNull(target, "The provided target must not be null.");
		this.target = target;
		this.credentialsProviderCommand = credentialsProviderCommand;
		attemptConnection();
	}

	private void attemptConnection() throws Exception {
		SkipperClient skipperClient = null;

		try {
			final HttpClientConfigurer httpClientConfigurer = HttpClientConfigurer.create()
					.targetHost(this.getTarget().getTargetUri())
					.skipTlsCertificateVerification(this.getTarget().isSkipSslValidation());
			if (this.getTarget().getTargetCredentials() != null
					&& StringUtils.hasText(this.getTarget().getTargetCredentials().getUsername())
					&& StringUtils.hasText(this.getTarget().getTargetCredentials().getPassword())) {
				httpClientConfigurer.basicAuthCredentials(this.getTarget().getTargetCredentials().getUsername(),
						this.getTarget().getTargetCredentials().getPassword());
			}
			if (StringUtils.hasText(credentialsProviderCommand)) {
				this.getTarget().setTargetCredentials(new TargetCredentials(true));
				final Resource credentialsResource = new ProcessOutputResource(
						credentialsProviderCommand.split("\\s+"));
				httpClientConfigurer.addInterceptor(new ResourceBasedAuthorizationInterceptor(credentialsResource));
			}
			this.restTemplate.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());

			String uri = this.getTarget().getTargetUri().toURL().toString();
			skipperClient = new DefaultSkipperClient(uri, this.restTemplate);
			// Actually attempt connection
			skipperClient.info();

			this.getTarget()
					.setTargetResultMessage(String.format("Successfully targeted %s", uri));

		}
		catch (Exception e) {
			this.getTarget().setTargetException(e);
			skipperClient = null;
			throw e;
		}
		finally {
			applicationEventPublisher.publishEvent(new SkipperClientUpdatedEvent(skipperClient));
		}
		// Other commands will be notified of the new SkipperClient object (may be null)

	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
