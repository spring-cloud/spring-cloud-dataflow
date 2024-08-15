/*
 * Copyright 2020-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.container.registry.ContainerImageRestTemplateFactory;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryAutoConfiguration;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryConfiguration;
import org.springframework.cloud.dataflow.container.registry.authorization.DockerOAuth2RegistryAuthorizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ApplicationConfigurationMetadataResolverAutoConfigurationTest.TestConfig.class)
@TestPropertySource(properties = {
		".dockerconfigjson={\"auths\":{\"demo.repository.io\":{\"username\":\"testuser\",\"password\":\"testpassword\",\"auth\":\"YWRtaW46SGFyYm9yMTIzNDU=\"}" +
				",\"demo2.repository.io\":{\"username\":\"testuser\",\"password\":\"testpassword\",\"auth\":\"YWRtaW46SGFyYm9yMTIzNDU=\"}}}",
		"spring.cloud.dataflow.container.registry-configurations[demorepositoryio].registry-host=demo.repository.io",
		"spring.cloud.dataflow.container.registry-configurations[demorepositoryio].disable-ssl-verification=true",

		"spring.cloud.dataflow.container.registry-configurations[demorepositoryio2].registry-host=demo2.repository.io",
		"spring.cloud.dataflow.container.registry-configurations[demorepositoryio2].disable-ssl-verification=true",
		"spring.cloud.dataflow.container.registry-configurations[demorepositoryio2].use-http-proxy=true",

		"spring.cloud.dataflow.container.registry-configurations[goharbor].registry-host=demo.goharbor.io",
		"spring.cloud.dataflow.container.registry-configurations[goharbor].authorization-type=dockeroauth2",
		"spring.cloud.dataflow.container.registry-configurations[goharbor].user=admin",
		"spring.cloud.dataflow.container.registry-configurations[goharbor].secret=Harbor12345",

		"spring.cloud.dataflow.container.registry-configurations[goharbor2].registry-host=demo2.goharbor.io",
		"spring.cloud.dataflow.container.registry-configurations[goharbor2].authorization-type=dockeroauth2",
		"spring.cloud.dataflow.container.registry-configurations[goharbor2].user=admin",
		"spring.cloud.dataflow.container.registry-configurations[goharbor2].secret=Harbor12345",
		"spring.cloud.dataflow.container.registry-configurations[goharbor2].use-http-proxy=true"
})
class ApplicationConfigurationMetadataResolverAutoConfigurationTest {

	@Autowired
	Map<String, ContainerRegistryConfiguration> registryConfigurationMap;

	@Autowired
	ContainerImageMetadataResolver containerImageMetadataResolver;

	@Autowired
	ContainerImageRestTemplateFactory containerImageRestTemplateFactory;

	@Autowired
	@Qualifier("noSslVerificationContainerRestTemplate")
	RestTemplate noSslVerificationContainerRestTemplate;

	@Autowired
	@Qualifier("containerRestTemplate")
	RestTemplate containerRestTemplate;

	@Autowired
	@Qualifier("noSslVerificationContainerRestTemplateWithHttpProxy")
	RestTemplate noSslVerificationContainerRestTemplateWithHttpProxy;

	@Autowired
	@Qualifier("containerRestTemplateWithHttpProxy")
	RestTemplate containerRestTemplateWithHttpProxy;

	@Test
	void registryConfigurationBeanCreationTest() {
		assertThat(registryConfigurationMap).hasSize(4);

		ContainerRegistryConfiguration secretConf = registryConfigurationMap.get("demo.repository.io");
		assertThat(secretConf).isNotNull();
		assertThat(secretConf.getRegistryHost()).isEqualTo("demo.repository.io");
		assertThat(secretConf.getAuthorizationType()).isEqualTo(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
		assertThat(secretConf.getUser()).isEqualTo("testuser");
		assertThat(secretConf.getSecret()).isEqualTo("testpassword");
		assertThat(secretConf.isDisableSslVerification())
				.describedAs("The explicit disable-ssl-verification=true property should augment the .dockerconfigjson based config")
				.isTrue();
		assertThat(secretConf.getExtra()).isNotEmpty();
		assertThat(secretConf.getExtra()).containsEntry(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY, "https://demo.repository.io/service/token?service=demo-registry&scope=repository:{repository}:pull");

		ContainerRegistryConfiguration secretConf2 = registryConfigurationMap.get("demo2.repository.io");
		assertThat(secretConf2).isNotNull();
		assertThat(secretConf2.getRegistryHost()).isEqualTo("demo2.repository.io");
		assertThat(secretConf2.getAuthorizationType()).isEqualTo(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
		assertThat(secretConf2.getUser()).isEqualTo("testuser");
		assertThat(secretConf2.getSecret()).isEqualTo("testpassword");
		assertThat(secretConf2.isDisableSslVerification())
				.describedAs("The explicit disable-ssl-verification=true property should augment the .dockerconfigjson based config")
				.isTrue();
		assertThat(secretConf2.getExtra()).isNotEmpty();
		assertThat(secretConf2.getExtra()).containsEntry(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY, "https://demo2.repository.io/service/token?service=demo-registry&scope=repository:{repository}:pull");

		ContainerRegistryConfiguration goharborConf = registryConfigurationMap.get("demo.goharbor.io");
		assertThat(goharborConf).isNotNull();
		assertThat(goharborConf.getRegistryHost()).isEqualTo("demo.goharbor.io");
		assertThat(goharborConf.getAuthorizationType()).isEqualTo(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
		assertThat(goharborConf.getUser()).isEqualTo("admin");
		assertThat(goharborConf.getSecret()).isEqualTo("Harbor12345");
		assertThat(goharborConf.isDisableSslVerification()).isFalse();
		assertThat(goharborConf.getExtra()).isNotEmpty();
		assertThat(goharborConf.getExtra()).containsEntry(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY, "https://demo.goharbor.io/service/token?service=demo-registry2&scope=repository:{repository}:pull");


		ContainerRegistryConfiguration goharborConf2 = registryConfigurationMap.get("demo2.goharbor.io");
		assertThat(goharborConf2).isNotNull();
		assertThat(goharborConf2.getRegistryHost()).isEqualTo("demo2.goharbor.io");
		assertThat(goharborConf2.getAuthorizationType()).isEqualTo(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
		assertThat(goharborConf2.getUser()).isEqualTo("admin");
		assertThat(goharborConf2.getSecret()).isEqualTo("Harbor12345");
		assertThat(goharborConf2.isDisableSslVerification()).isFalse();
		assertThat(goharborConf2.getExtra()).isNotEmpty();
		assertThat(goharborConf2.getExtra()).containsEntry(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY, "https://demo2.goharbor.io/service/token?service=demo-registry2&scope=repository:{repository}:pull");
	}

	@Test
	void containerImageMetadataResolverWithActiveSSL() throws URISyntaxException {
		assertThat(containerImageMetadataResolver).isNotNull();
		Map<String, String> labels = containerImageMetadataResolver.getImageLabels("demo.goharbor.io/test/image:1.0.0");
		assertThat(labels).containsExactly(Collections.singletonMap("foo", "bar").entrySet().iterator().next());

		// Determine the OAuth2 token service entry point.
		verify(noSslVerificationContainerRestTemplate)
				.exchange(eq(new URI("https://demo.goharbor.io/v2/")), eq(HttpMethod.GET), any(), eq(Map.class));

		// Get authorization token
		verify(containerRestTemplate).exchange(
				eq(new URI("https://demo.goharbor.io/service/token?service=demo-registry2&scope=repository:test/image:pull")),
				eq(HttpMethod.GET), any(), eq(Map.class));
		// Get Manifest
		verify(containerRestTemplate).exchange(eq(new URI("https://demo.goharbor.io/v2/test/image/manifests/1.0.0")),
				eq(HttpMethod.GET), any(), eq(Map.class));
		// Get Blobs
		verify(containerRestTemplate).exchange(eq(new URI("https://demo.goharbor.io/v2/test/image/blobs/test_digest")),
				eq(HttpMethod.GET), any(), eq(Map.class));
	}

	@Test
	void containerImageMetadataResolverWithDisabledSSL() throws URISyntaxException {
		assertThat(containerImageMetadataResolver).isNotNull();
		Map<String, String> labels = containerImageMetadataResolver.getImageLabels("demo.repository.io/disabledssl/image:1.0.0");
		assertThat(labels).containsExactly(Collections.singletonMap("foo", "bar").entrySet().iterator().next());

		// Determine the OAuth2 token service entry point.
		verify(noSslVerificationContainerRestTemplate)
				.exchange(eq(new URI("https://demo.repository.io/v2/")), eq(HttpMethod.GET), any(), eq(Map.class));

		// Get authorization token
		verify(noSslVerificationContainerRestTemplate).exchange(
				eq(new URI("https://demo.repository.io/service/token?service=demo-registry&scope=repository:disabledssl/image:pull")),
				eq(HttpMethod.GET), any(), eq(Map.class));
		// Get Manifest
		verify(noSslVerificationContainerRestTemplate).exchange(eq(new URI("https://demo.repository.io/v2/disabledssl/image/manifests/1.0.0")),
				eq(HttpMethod.GET), any(), eq(Map.class));
		// Get Blobs
		verify(noSslVerificationContainerRestTemplate).exchange(eq(new URI("https://demo.repository.io/v2/disabledssl/image/blobs/test_digest")),
				eq(HttpMethod.GET), any(), eq(Map.class));
	}

	@ImportAutoConfiguration({ ContainerRegistryAutoConfiguration.class, ApplicationConfigurationMetadataResolverAutoConfiguration.class })
	static class TestConfig {

		@Bean
		@ConditionalOnMissingBean(name = "containerImageRestTemplateFactory")
		public ContainerImageRestTemplateFactory containerImageRestTemplateFactory(
				@Qualifier("noSslVerificationContainerRestTemplate") RestTemplate noSslVerificationContainerRestTemplate,
				@Qualifier("noSslVerificationContainerRestTemplateWithHttpProxy") RestTemplate noSslVerificationContainerRestTemplateWithHttpProxy,
				@Qualifier("containerRestTemplate") RestTemplate containerRestTemplate,
				@Qualifier("containerRestTemplateWithHttpProxy") RestTemplate containerRestTemplateWithHttpProxy) {
			ContainerImageRestTemplateFactory containerImageRestTemplateFactory = Mockito.mock(ContainerImageRestTemplateFactory.class);
			when(containerImageRestTemplateFactory.getContainerRestTemplate(eq(true), eq(false),
				anyMap()))
				.thenReturn(noSslVerificationContainerRestTemplate);
			when(containerImageRestTemplateFactory.getContainerRestTemplate(eq(true), eq(true),
				anyMap()))
				.thenReturn(noSslVerificationContainerRestTemplateWithHttpProxy);
			when(containerImageRestTemplateFactory.getContainerRestTemplate(eq(false), eq(false),
				anyMap()))
				.thenReturn(containerRestTemplate);
			when(containerImageRestTemplateFactory.getContainerRestTemplate(eq(false), eq(true),
				anyMap()))
				.thenReturn(containerRestTemplateWithHttpProxy);
			return containerImageRestTemplateFactory;
		}

		@Bean(name = "noSslVerificationContainerRestTemplate")
		RestTemplate noSslVerificationContainerRestTemplate() throws URISyntaxException, JsonProcessingException {
			RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

			// demo.repository.io
			HttpHeaders authenticateHeader = new HttpHeaders();
			authenticateHeader.add("Www-Authenticate", "Bearer realm=\"https://demo.repository.io/service/token\",service=\"demo-registry\",scope=\"registry:category:pull\"");
			HttpClientErrorException httpClientErrorException =
					HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "", authenticateHeader, new byte[0], null);

			when(restTemplate.exchange(eq(new URI("https://demo.repository.io/v2/")),
					eq(HttpMethod.GET), any(), eq(Map.class))).thenThrow(httpClientErrorException);


			when(restTemplate
					.exchange(
							eq(new URI("https://demo.repository.io/service/token?service=demo-registry&scope=repository:disabledssl/image:pull")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(Collections.singletonMap("token", "my_token_999"), HttpStatus.OK));

			when(restTemplate
					.exchange(
							eq(new URI("https://demo.repository.io/v2/disabledssl/image/manifests/1.0.0")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(Collections.singletonMap("config", Collections.singletonMap("digest", "test_digest")), HttpStatus.OK));

			when(restTemplate
					.exchange(
							eq(new URI("https://demo.repository.io/v2/disabledssl/image/blobs/test_digest")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(blobValue(), HttpStatus.OK));

			// demo.goharbor.io
			HttpHeaders authenticateHeader2 = new HttpHeaders();
			authenticateHeader2.add("Www-Authenticate", "Bearer realm=\"https://demo.goharbor.io/service/token\",service=\"demo-registry2\",scope=\"registry:category:pull\"");
			HttpClientErrorException httpClientErrorException2 =
					HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "", authenticateHeader2, new byte[0], null);

			when(restTemplate.exchange(eq(new URI("https://demo.goharbor.io/v2/")),
					eq(HttpMethod.GET), any(), eq(Map.class))).thenThrow(httpClientErrorException2);

			return restTemplate;
		}

		@Bean(name = "containerRestTemplate")
		RestTemplate containerRestTemplate() throws URISyntaxException, JsonProcessingException {
			RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

			when(restTemplate
					.exchange(
							eq(new URI("https://demo.goharbor.io/service/token?service=demo-registry2&scope=repository:test/image:pull")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(Collections.singletonMap("token", "my_token_999"), HttpStatus.OK));

			when(restTemplate
					.exchange(
							eq(new URI("https://demo.goharbor.io/v2/test/image/manifests/1.0.0")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(Collections.singletonMap("config", Collections.singletonMap("digest", "test_digest")), HttpStatus.OK));

			when(restTemplate
					.exchange(
							eq(new URI("https://demo.goharbor.io/v2/test/image/blobs/test_digest")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(blobValue(), HttpStatus.OK));

			return restTemplate;
		}

		@Bean(name = "containerRestTemplateWithHttpProxy")
		RestTemplate containerRestTemplateWithHttpProxy() throws URISyntaxException, JsonProcessingException {
			RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

			when(restTemplate
					.exchange(
							eq(new URI("https://demo2.goharbor.io/service/token?service=demo-registry2&scope=repository:test/image:pull")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(Collections.singletonMap("token", "my_token_999"), HttpStatus.OK));

			when(restTemplate
					.exchange(
							eq(new URI("https://demo2.goharbor.io/v2/test/image/manifests/1.0.0")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(Collections.singletonMap("config", Collections.singletonMap("digest", "test_digest")), HttpStatus.OK));

			when(restTemplate
					.exchange(
							eq(new URI("https://demo2.goharbor.io/v2/test/image/blobs/test_digest")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(blobValue(), HttpStatus.OK));

			return restTemplate;
		}

		@Bean(name = "noSslVerificationContainerRestTemplateWithHttpProxy")
		RestTemplate noSslVerificationContainerRestTemplateWithHttpProxy() throws URISyntaxException, JsonProcessingException {
			RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

			// demo.repository.io
			HttpHeaders authenticateHeader = new HttpHeaders();
			authenticateHeader.add("Www-Authenticate", "Bearer realm=\"https://demo2.repository.io/service/token\",service=\"demo-registry\",scope=\"registry:category:pull\"");
			HttpClientErrorException httpClientErrorException =
					HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "", authenticateHeader, new byte[0], null);

			when(restTemplate.exchange(eq(new URI("https://demo2.repository.io/v2/")),
					eq(HttpMethod.GET), any(), eq(Map.class))).thenThrow(httpClientErrorException);


			when(restTemplate
					.exchange(
							eq(new URI("https://demo2.repository.io/service/token?service=demo-registry&scope=repository:disabledssl/image:pull")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(Collections.singletonMap("token", "my_token_999"), HttpStatus.OK));

			when(restTemplate
					.exchange(
							eq(new URI("https://demo2.repository.io/v2/disabledssl/image/manifests/1.0.0")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(Collections.singletonMap("config", Collections.singletonMap("digest", "test_digest")), HttpStatus.OK));

			when(restTemplate
					.exchange(
							eq(new URI("https://demo2.repository.io/v2/disabledssl/image/blobs/test_digest")),
							eq(HttpMethod.GET), any(), eq(Map.class)))
					.thenReturn(new ResponseEntity<>(blobValue(), HttpStatus.OK));

			// demo.goharbor.io
			HttpHeaders authenticateHeader2 = new HttpHeaders();
			authenticateHeader2.add("Www-Authenticate", "Bearer realm=\"https://demo2.goharbor.io/service/token\",service=\"demo-registry2\",scope=\"registry:category:pull\"");
			HttpClientErrorException httpClientErrorException2 =
					HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "", authenticateHeader2, new byte[0], null);

			when(restTemplate.exchange(eq(new URI("https://demo2.goharbor.io/v2/")),
					eq(HttpMethod.GET), any(), eq(Map.class))).thenThrow(httpClientErrorException2);

			return restTemplate;
		}

		public Map blobValue() throws JsonProcessingException {
			return new ObjectMapper().readValue("{\"config\": {\"Labels\": {\"foo\": \"bar\"} } }", Map.class);
		}

	}
}
