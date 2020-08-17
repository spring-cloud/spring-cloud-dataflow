/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.cloud.dataflow.container.registry;

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

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.dataflow.container.registry.authorization.AnonymousRegistryAuthorizer;
import org.springframework.cloud.dataflow.container.registry.authorization.AwsEcrAuthorizer;
import org.springframework.cloud.dataflow.container.registry.authorization.BasicAuthRegistryAuthorizer;
import org.springframework.cloud.dataflow.container.registry.authorization.DockerConfigJsonSecretToRegistryConfigurationConverter;
import org.springframework.cloud.dataflow.container.registry.authorization.DockerOAuth2RegistryAuthorizer;
import org.springframework.cloud.dataflow.container.registry.authorization.DropAuthorizationHeaderOnSignedS3RequestRedirectStrategy;
import org.springframework.cloud.dataflow.container.registry.authorization.RegistryAuthorizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Auto configuration for Container Registry.
 *
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@EnableConfigurationProperties({ ContainerRegistryProperties.class })
public class ContainerRegistryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(ContainerRegistryAutoConfiguration.class);

	@Bean
	public RegistryAuthorizer dockerOAuth2RegistryAuthorizer(
			@Qualifier("containerRestTemplate") RestTemplate containerRestTemplate,
			@Qualifier("noSslVerificationContainerRestTemplate") RestTemplate noSslVerificationContainerRestTemplate) {
		return new DockerOAuth2RegistryAuthorizer(containerRestTemplate, noSslVerificationContainerRestTemplate);
	}

	@Bean
	public RegistryAuthorizer anonymousRegistryAuthorizer() {
		return new AnonymousRegistryAuthorizer();
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
	public ContainerImageParser containerImageParser(ContainerRegistryProperties properties) {
		return new ContainerImageParser(properties.getDefaultRegistryHost(),
				properties.getDefaultRepositoryTag(), properties.getOfficialRepositoryNamespace());
	}

	@Bean
	@ConditionalOnMissingBean
	public ContainerRegistryService containerRegistryService(
			@Qualifier("containerRestTemplate") RestTemplate containerRestTemplate,
			@Qualifier("noSslVerificationContainerRestTemplate") RestTemplate trustAnySslRestTemplate,
			ContainerImageParser containerImageParser,
			Map<String, ContainerRegistryConfiguration> registryConfigurationMap,
			List<RegistryAuthorizer> registryAuthorizers) {
		return new ContainerRegistryService(containerRestTemplate, trustAnySslRestTemplate,
				containerImageParser, registryConfigurationMap, registryAuthorizers);
	}

	@Bean
	public Map<String, ContainerRegistryConfiguration> registryConfigurationMap(ContainerRegistryProperties properties,
			@Value("${.dockerconfigjson:#{null}}") String dockerConfigJsonSecret,
			DockerConfigJsonSecretToRegistryConfigurationConverter secretToRegistryConfigurationConverter) {

		// Retrieve registry configurations, explicitly declared via properties.
		Map<String, ContainerRegistryConfiguration> registryConfigurationMap =
				properties.getRegistryConfigurations().entrySet().stream()
						.collect(Collectors.toMap(e -> e.getValue().getRegistryHost(), Map.Entry::getValue));

		// For dockeroauth2 configuration that doesn't have the Docker OAuth2 Access Token entry point set,
		// use the secretToRegistryConfigurationConverter.getDockerTokenServiceUri() tor retrieve the entry point.
		registryConfigurationMap.values().stream()
				.filter(rc -> rc.getAuthorizationType()
						== ContainerRegistryConfiguration.AuthorizationType.dockeroauth2)
				.filter(rc -> !rc.getExtra().containsKey(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY))
				.forEach(rc -> secretToRegistryConfigurationConverter.getDockerTokenServiceUri(rc.getRegistryHost())
						.ifPresent(tokenServiceUri -> rc.getExtra().put(
								DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY,
								tokenServiceUri)));

		if (!StringUtils.isEmpty(dockerConfigJsonSecret)) {
			// Retrieve registry configurations from mounted kubernetes Secret.
			Map<String, ContainerRegistryConfiguration> secretsRegistryConfigurationMap
					= secretToRegistryConfigurationConverter.convert(dockerConfigJsonSecret);

			if (!CollectionUtils.isEmpty(secretsRegistryConfigurationMap)) {
				// Merge the Secret and the Property based registry configurations.
				// The properties values when set has precedence over the Secret retrieved one. Later allow to override
				// some of the Secret properties or set the disableSslVerification for secret based configs.
				registryConfigurationMap = Stream.concat(
						secretsRegistryConfigurationMap.entrySet().stream(),
						registryConfigurationMap.entrySet().stream())
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								Map.Entry::getValue,
								(secretConf, propConf) -> {
									ContainerRegistryConfiguration rc = new ContainerRegistryConfiguration();
									rc.setRegistryHost(secretConf.getRegistryHost());
									rc.setUser(StringUtils.hasText(propConf.getUser()) ?
											propConf.getUser() :
											secretConf.getUser());
									rc.setSecret(StringUtils.hasText(propConf.getSecret()) ?
											propConf.getSecret() :
											secretConf.getSecret());
									rc.setAuthorizationType(propConf.getAuthorizationType() != null ?
											propConf.getAuthorizationType() :
											secretConf.getAuthorizationType());
									rc.setManifestMediaType(StringUtils.hasText(propConf.getManifestMediaType()) ?
											propConf.getManifestMediaType() :
											secretConf.getManifestMediaType());
									rc.setDisableSslVerification(propConf.isDisableSslVerification());
									rc.getExtra().putAll(secretConf.getExtra());
									rc.getExtra().putAll(propConf.getExtra());
									return rc;
								}
						));
			}
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
	@ConditionalOnMissingBean(name = "containerRestTemplate")
	public RestTemplate containerRestTemplate(RestTemplateBuilder builder) {
		// Create a RestTemplate that uses custom request factory
		return this.initRestTemplate(builder, HttpClients.custom());
	}

	@Bean
	@ConditionalOnMissingBean(name = "noSslVerificationContainerRestTemplate")
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

		// Create a RestTemplate that uses custom request factory
		return initRestTemplate(builder,
				HttpClients.custom()
						.setSSLContext(sslContext)
						.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE));
	}

	private RestTemplate initRestTemplate(RestTemplateBuilder restTemplateBuilder, HttpClientBuilder clientBuilder) {
		StringHttpMessageConverter octetToStringMessageConverter = new StringHttpMessageConverter();
		List<MediaType> mediaTypeList = new ArrayList(octetToStringMessageConverter.getSupportedMediaTypes());
		mediaTypeList.add(MediaType.APPLICATION_OCTET_STREAM);
		octetToStringMessageConverter.setSupportedMediaTypes(mediaTypeList);

		clientBuilder.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build());

		HttpComponentsClientHttpRequestFactory customRequestFactory =
				new HttpComponentsClientHttpRequestFactory(
						clientBuilder
								.setRedirectStrategy(new DropAuthorizationHeaderOnSignedS3RequestRedirectStrategy())
								.build());

		return restTemplateBuilder
				.additionalMessageConverters(octetToStringMessageConverter)
				.requestFactory(() -> customRequestFactory)
				.build();
	}
}
