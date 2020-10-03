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

package org.springframework.cloud.dataflow.configuration.metadata;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.container.RegistryConfiguration;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.DockerOAuth2RegistryAuthorizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Christian Tzolov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ApplicationConfigurationMetadataResolverAutoConfigurationTest.TestConfig.class)
@TestPropertySource(properties = {
		".dockerconfigjson={\"auths\":{\"demo.repository.io\":{\"username\":\"testuser\",\"password\":\"testpassword\",\"auth\":\"YWRtaW46SGFyYm9yMTIzNDU=\"}}}",
		"spring.cloud.dataflow.container.registry-configurations[demorepositoryio].registry-host=demo.repository.io",
		"spring.cloud.dataflow.container.registry-configurations[demorepositoryio].disable-ssl-verification=true",

		"spring.cloud.dataflow.container.registry-configurations[goharbor].registry-host=demo.goharbor.io",
		"spring.cloud.dataflow.container.registry-configurations[goharbor].authorization-type=dockeroauth2",
		"spring.cloud.dataflow.container.registry-configurations[goharbor].user=admin",
		"spring.cloud.dataflow.container.registry-configurations[goharbor].secret=Harbor12345"
})
public class ApplicationConfigurationMetadataResolverAutoConfigurationTest {

	@Autowired
	Map<String, RegistryConfiguration> registryConfigurationMap;

	@Autowired
	ContainerImageMetadataResolver containerImageMetadataResolver;

	@Autowired
	@Qualifier("noSslVerificationContainerRestTemplate")
	RestTemplate noSslVerificationContainerRestTemplate;

	@Autowired
	@Qualifier("containerRestTemplate")
	RestTemplate containerRestTemplate;

	@Test
	public void registryConfigurationBeanCreationTest() {
		assertThat(registryConfigurationMap).hasSize(2);

		RegistryConfiguration secretConf = registryConfigurationMap.get("demo.repository.io");
		assertThat(secretConf).isNotNull();
		assertThat(secretConf.getRegistryHost()).isEqualTo("demo.repository.io");
		assertThat(secretConf.getAuthorizationType()).isEqualTo(RegistryConfiguration.AuthorizationType.dockeroauth2);
		assertThat(secretConf.getUser()).isEqualTo("testuser");
		assertThat(secretConf.getSecret()).isEqualTo("testpassword");
		assertThat(secretConf.isDisableSslVerification())
				.describedAs("The explicit disable-ssl-verification=true property should augment the .dockerconfigjson based config")
				.isTrue();
		assertThat(secretConf.getExtra()).isNotEmpty();
		assertThat(secretConf.getExtra().get(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY))
				.isEqualTo("https://demo.repository.io/service/token?service=demo-registry&scope=repository:{repository}:pull");

		RegistryConfiguration goharborConf = registryConfigurationMap.get("demo.goharbor.io");
		assertThat(goharborConf).isNotNull();
		assertThat(goharborConf.getRegistryHost()).isEqualTo("demo.goharbor.io");
		assertThat(goharborConf.getAuthorizationType()).isEqualTo(RegistryConfiguration.AuthorizationType.dockeroauth2);
		assertThat(goharborConf.getUser()).isEqualTo("admin");
		assertThat(goharborConf.getSecret()).isEqualTo("Harbor12345");
		assertThat(goharborConf.isDisableSslVerification()).isFalse();
		assertThat(goharborConf.getExtra()).isNotEmpty();
		assertThat(goharborConf.getExtra().get(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY))
				.isEqualTo("https://demo.goharbor.io/service/token?service=demo-registry2&scope=repository:{repository}:pull");
	}

	@Test
	public void containerImageMetadataResolverWithActiveSSL() throws URISyntaxException {
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
				eq(HttpMethod.GET), any(), eq(String.class));
	}

	@Test
	public void containerImageMetadataResolverWithDisabledSSL() throws URISyntaxException {
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
				eq(HttpMethod.GET), any(), eq(String.class));
	}

	@ImportAutoConfiguration(ApplicationConfigurationMetadataResolverAutoConfiguration.class)
	static class TestConfig {

		@Bean(name = "noSslVerificationContainerRestTemplate")
		RestTemplate noSslVerificationContainerRestTemplate() throws URISyntaxException {
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
							eq(HttpMethod.GET), any(), eq(String.class)))
					.thenReturn(new ResponseEntity<>("{\"config\": {\"Labels\": {\"foo\": \"bar\"} } }", HttpStatus.OK));

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
		RestTemplate containerRestTemplate() throws URISyntaxException {
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
							eq(HttpMethod.GET), any(), eq(String.class)))
					.thenReturn(new ResponseEntity<>("{\"config\": {\"Labels\": {\"foo\": \"bar\"} } }", HttpStatus.OK));

			return restTemplate;
		}
	}
}
