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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.DockerConfigJsonSecretToRegistryConfigurationConverter;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.DockerOAuth2RegistryAuthorizer;
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
	public RegistryAuthorizer dockerOAuth2RegistryAuthorizer() {
		return new DockerOAuth2RegistryAuthorizer();
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
			@Qualifier("containerRestTemplate") RestTemplate metadataRestTemplate,
			@Qualifier("noSslVerificationContainerRestTemplate") RestTemplate trustAnySslRestTemplate,
			ContainerImageParser imageNameParser,
			Map<String, RegistryConfiguration> registryConfigurationMap,
			List<RegistryAuthorizer> registryAuthorizers) {
		return new DefaultContainerImageMetadataResolver(metadataRestTemplate, trustAnySslRestTemplate, imageNameParser,
				registryConfigurationMap, registryAuthorizers);
	}

	@Bean
	@ConditionalOnMissingBean(ApplicationConfigurationMetadataResolver.class)
	public ApplicationConfigurationMetadataResolver metadataResolver(
			DefaultContainerImageMetadataResolver containerImageMetadataResolver) {
		return new BootApplicationConfigurationMetadataResolver(containerImageMetadataResolver);
	}

	@Bean
	public Map<String, RegistryConfiguration> registryConfigurationMap(ContainerImageMetadataProperties properties,
			@Value("${.dockerconfigjson:null}") String dockerConfigJsonSecret,
			DockerConfigJsonSecretToRegistryConfigurationConverter secretToRegistryConfigurationConverter) {

		// Retrieve registry configurations, explicitly declared via properties.
		Map<String, RegistryConfiguration> registryConfigurationMap =
				properties.getRegistryConfigurations().entrySet().stream()
						.collect(Collectors.toMap(e -> e.getValue().getRegistryHost(), Map.Entry::getValue));

		if (!StringUtils.isEmpty(dockerConfigJsonSecret)) {
			// Retrieve registry configurations from mounted kubernetes Secret.
			Map<String, RegistryConfiguration> secretsRegistryConfigurationMap
					= secretToRegistryConfigurationConverter.convert(dockerConfigJsonSecret);

			// Merge the Secret and the Property based registry configurations.
			Map<String, RegistryConfiguration> result = Stream.concat(
					secretsRegistryConfigurationMap.entrySet().stream(),
					registryConfigurationMap.entrySet().stream())
					.collect(Collectors.toMap(
							Map.Entry::getKey,
							Map.Entry::getValue,
							(secretConf, propConf) -> {
								RegistryConfiguration rc = new RegistryConfiguration();
								rc.setRegistryHost(secretConf.getRegistryHost());
								rc.setUser(StringUtils.hasText(propConf.getUser()) ? propConf.getUser() : secretConf.getUser());
								rc.setSecret(StringUtils.hasText(propConf.getSecret()) ? propConf.getSecret() : secretConf.getSecret());
								rc.setAuthorizationType(propConf.getAuthorizationType() != null ? propConf.getAuthorizationType() : secretConf.getAuthorizationType());
								rc.setManifestMediaType(StringUtils.hasText(propConf.getManifestMediaType()) ? propConf.getManifestMediaType() : secretConf.getManifestMediaType());
								rc.setDisableSslVerification(propConf.isDisableSslVerification());
								rc.getExtra().putAll(secretConf.getExtra());
								rc.getExtra().putAll(propConf.getExtra());
								return rc;
							}
					));
			registryConfigurationMap = result;
		}

		logger.info("Final Registry Configurations: " + registryConfigurationMap);

		return registryConfigurationMap;
	}

	@Bean
	public DockerConfigJsonSecretToRegistryConfigurationConverter secretToRegistryConfigurationConverter(
			@Qualifier("noSslVerificationContainerRestTemplate") RestTemplate trustAnySslRestTemplate) {
		return new DockerConfigJsonSecretToRegistryConfigurationConverter(trustAnySslRestTemplate);
	}

	@Bean
	@ConditionalOnMissingBean
	public RestTemplate containerRestTemplate(RestTemplateBuilder builder) {
		return this.initRestTemplateBuilder(builder).build();
	}

	@Bean
	public RestTemplate noSslVerificationContainerRestTemplate(RestTemplateBuilder builder)
			throws NoSuchAlgorithmException, KeyManagementException {

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
		return this.initRestTemplateBuilder(builder).requestFactory(() -> customRequestFactory).build();
	}

	private RestTemplateBuilder initRestTemplateBuilder(RestTemplateBuilder builder) {
		StringHttpMessageConverter octetToStringMessageConverter = new StringHttpMessageConverter();
		List<MediaType> mediaTypeList = new ArrayList(octetToStringMessageConverter.getSupportedMediaTypes());
		mediaTypeList.add(MediaType.APPLICATION_OCTET_STREAM);
		octetToStringMessageConverter.setSupportedMediaTypes(mediaTypeList);

		return builder.additionalMessageConverters(octetToStringMessageConverter);
	}
}
