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

import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenResult;

import org.springframework.cloud.dataflow.container.registry.ContainerImage;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Christian Tzolov
 */
public class AwsEcrAuthorizer implements RegistryAuthorizer {

	public static final String AWS_REGION = "region";
	public static final String REGISTRY_IDS = "registryIds";

	@Override
	public ContainerRegistryConfiguration.AuthorizationType getType() {
		return ContainerRegistryConfiguration.AuthorizationType.awsecr;
	}

	@Override
	public HttpHeaders getAuthorizationHeaders(ContainerImage containerImage,
			ContainerRegistryConfiguration registryConfiguration) {
		return getAuthorizationHeaders(registryConfiguration, null);
	}

	@Override
	public HttpHeaders getAuthorizationHeaders(ContainerRegistryConfiguration registryConfiguration,
			Map<String, String> configProperties) {

		Assert.isTrue(registryConfiguration.getAuthorizationType() == this.getType(),
				"Incorrect type: " + registryConfiguration.getAuthorizationType());

		AmazonECRClientBuilder ecrBuilder = AmazonECRClientBuilder.standard();
		if (registryConfiguration.getExtra().containsKey(AWS_REGION)) {
			ecrBuilder.withRegion(registryConfiguration.getExtra().get(AWS_REGION));
		}
		if (StringUtils.hasText(registryConfiguration.getUser()) && StringUtils.hasText(registryConfiguration.getSecret())) {
			// Expects that the 'user' == 'Access Key ID' and 'secret' == 'Secret Access Key'
			ecrBuilder.withCredentials(new AWSStaticCredentialsProvider(
					new BasicAWSCredentials(registryConfiguration.getUser(), registryConfiguration.getSecret())));
		}

		AmazonECR client = ecrBuilder.build();

		GetAuthorizationTokenRequest request = new GetAuthorizationTokenRequest();
		if (registryConfiguration.getExtra().containsKey(REGISTRY_IDS)) {
			request.withRegistryIds(registryConfiguration.getExtra().get(REGISTRY_IDS).split(","));
		}
		GetAuthorizationTokenResult response = client.getAuthorizationToken(request);
		String token = response.getAuthorizationData().iterator().next().getAuthorizationToken();
		final HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(token);
		return headers;
	}
}
