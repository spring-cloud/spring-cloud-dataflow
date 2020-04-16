/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataProperties;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageParser;
import org.springframework.cloud.dataflow.configuration.metadata.container.DefaultContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.container.RegistryConfiguration;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.AwsEcrAuthorizer;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.BasicAuthRegistryAuthorizer;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.DockerHubRegistryAuthorizer;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.DockerConfigJsonSecretToRegistryConfigurationConverter;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.RegistryAuthorizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

/**
 * Automatically exposes an {@link ApplicationConfigurationMetadataResolver} if none is already registered.
 *
 * @author Eric Bottard
 * @author Christian Tzolov
 */
@Configuration
public class ApplicationConfigurationMetadataResolverAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationConfigurationMetadataResolverAutoConfiguration.class);

	@Bean
	public RegistryAuthorizer dockerHubRegistryAuthorizer() {
		return new DockerHubRegistryAuthorizer();
	}

	@Bean
	public RegistryAuthorizer basicAuthRegistryAuthorizer() {
		return new BasicAuthRegistryAuthorizer();
	}

	@Bean
	public RegistryAuthorizer awsRegistryAuthorizer() {
		return new AwsEcrAuthorizer();
	}

	@Bean
	public ContainerImageParser containerImageParser(ContainerImageMetadataProperties properties) {
		return new ContainerImageParser(properties.getDefaultRegistryHost(),
				properties.getDefaultRepositoryTag(), properties.getOfficialRepositoryNamespace());
	}

	@Bean
	@Validated
	public ContainerImageMetadataProperties containerImageMetadataProperties() {
		return new ContainerImageMetadataProperties();
	}

	@Bean
	@ConditionalOnMissingBean(ContainerImageMetadataResolver.class)
	public DefaultContainerImageMetadataResolver containerImageMetadataResolver(
			@Qualifier("metadataRestTemplate") RestTemplate metadataRestTemplate,
			ContainerImageParser imageNameParser,
			Map<String, RegistryConfiguration> registryConfigurationMap,
			List<RegistryAuthorizer> registryAuthorizers) {
		return new DefaultContainerImageMetadataResolver(metadataRestTemplate, imageNameParser,
				registryConfigurationMap, registryAuthorizers);
	}

	@Bean
	@ConditionalOnMissingBean(ApplicationConfigurationMetadataResolver.class)
	public ApplicationConfigurationMetadataResolver metadataResolver(
			DefaultContainerImageMetadataResolver containerImageMetadataResolver) {
		return new BootApplicationConfigurationMetadataResolver(containerImageMetadataResolver);
	}

	@Bean
	@ConditionalOnMissingBean
	public RestTemplate metadataRestTemplate(RestTemplateBuilder builder, ContainerImageMetadataProperties properties)
			throws NoSuchAlgorithmException, KeyManagementException {

		StringHttpMessageConverter octetToStringMessageConverter = new StringHttpMessageConverter();
		List<MediaType> mediaTypeList = new ArrayList(octetToStringMessageConverter.getSupportedMediaTypes());
		mediaTypeList.add(MediaType.APPLICATION_OCTET_STREAM);
		octetToStringMessageConverter.setSupportedMediaTypes(mediaTypeList);

		if (!properties.isDisableSslVerification()) {
			return builder.additionalMessageConverters(octetToStringMessageConverter).build();
		}

		// From https://www.javacodemonk.com/disable-ssl-certificate-check-resttemplate-e2c53583

		// Trust manager that blindly trusts all SSL certificates.
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}

					public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}
				}
		};
		SSLContext sslContext = SSLContext.getInstance("SSL");
		// Install trust manager to SSL Context.
		sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		// Create an HttpClient that uses the custom SSLContext and do not verify cert hostname.
		CloseableHttpClient httpClient = HttpClients.custom().setSSLContext(sslContext)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
		HttpComponentsClientHttpRequestFactory customRequestFactory = new HttpComponentsClientHttpRequestFactory();
		customRequestFactory.setHttpClient(httpClient);
		// Create a RestTemplate that uses custom request factory
		return builder.requestFactory(() -> customRequestFactory)
				.additionalMessageConverters(octetToStringMessageConverter).build();
	}

	@Bean
	public Map<String, RegistryConfiguration> registryConfigurationMap(ContainerImageMetadataProperties properties,
			@Value("${.dockerconfigjson:null}") String dockerConfigJsonSecret,
			DockerConfigJsonSecretToRegistryConfigurationConverter secretToRegistryConfigurationConverter) {

		HashMap<String, RegistryConfiguration> registryConfigurationMap = new HashMap<>();

		// Retrieve registry configurations, explicitly declared via properties.
		Map<String, RegistryConfiguration> propertiesRegistryConfigurationMap = properties.getRegistryConfigurations().stream()
				.collect(Collectors.toMap(RegistryConfiguration::getRegistryHost, Function.identity()));

		registryConfigurationMap.putAll(propertiesRegistryConfigurationMap);

		if (!StringUtils.isEmpty(dockerConfigJsonSecret)) {
			// Retrieve registry configurations from mounted kubernetes Secret.
			Map<String, RegistryConfiguration> secretsRegistryConfigurationMap
					= secretToRegistryConfigurationConverter.convert(dockerConfigJsonSecret);
			registryConfigurationMap.putAll(secretsRegistryConfigurationMap);
		}

		logger.info("Registry Configurations: " + registryConfigurationMap);

		return registryConfigurationMap;
	}

	@Bean
	public DockerConfigJsonSecretToRegistryConfigurationConverter secretToRegistryConfigurationConverter(RestTemplate restTemplate) {
		return new DockerConfigJsonSecretToRegistryConfigurationConverter(restTemplate);
	}
}
