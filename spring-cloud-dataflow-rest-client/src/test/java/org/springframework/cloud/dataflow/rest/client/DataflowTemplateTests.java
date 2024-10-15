/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.cloud.dataflow.rest.job.StepExecutionHistory;
import org.springframework.cloud.dataflow.rest.resource.RootResource;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
class DataflowTemplateTests {

	private ObjectMapper mapper;

	@BeforeEach
	void setup() {
		mapper = new ObjectMapper();
		mapper.registerModule(new Jdk8Module());
		mapper.registerModule(new Jackson2HalModule());
		mapper.registerModule(new JavaTimeModule());
		mapper.registerModule(new Jackson2DataflowModule());
		System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(100));
	}

	@AfterEach
	void shutdown() {
		System.clearProperty("sun.net.client.defaultConnectTimeout");
	}

	@Test
	void dataFlowTemplateContructorWithNullUri() throws URISyntaxException {

		try {
			new DataFlowTemplate(null, mapper);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The provided baseURI must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void dataFlowTemplateContructorWithNonExistingUri() throws URISyntaxException {
		assertThatExceptionOfType(ResourceAccessException.class).isThrownBy(() -> {
			new DataFlowTemplate(new URI("https://doesnotexist:1234"), mapper);
		});
	}

	@Test
	void thatObjectMapperGetsPrepared() {
		final ObjectMapper objectMapper = new ObjectMapper();
		DataFlowTemplate.prepareObjectMapper(objectMapper);
		assertCorrectMixins(objectMapper);
	}

	@Test
	void prepareObjectMapperWithNullObjectMapper() {
		try {
			DataFlowTemplate.prepareObjectMapper(null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The objectMapper must not be null.");
			return;
		}
	}

	@Test
	void thatDefaultDataflowRestTemplateContainsMixins() {
		final RestTemplate restTemplate = DataFlowTemplate.getDefaultDataflowRestTemplate();

		assertThat(restTemplate).isNotNull();
		assertThat(restTemplate.getErrorHandler() instanceof VndErrorResponseErrorHandler).isTrue();

		assertCorrectMixins(restTemplate);

	}

	private void assertCorrectMixins(RestTemplate restTemplate) {
		boolean containsMappingJackson2HttpMessageConverter = false;

		for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
			if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
				containsMappingJackson2HttpMessageConverter = true;
				assertCorrectMixins(jacksonConverter.getObjectMapper());
			}
		}

		if (!containsMappingJackson2HttpMessageConverter) {
			fail("Expected that the restTemplate's list of Message Converters contained a "
					+ "MappingJackson2HttpMessageConverter");
		}
	}

	private void assertCorrectMixins(ObjectMapper objectMapper) {
		assertThat(objectMapper.findMixInClassFor(JobExecution.class)).isNotNull();
		assertThat(objectMapper.findMixInClassFor(JobParameters.class)).isNotNull();
		assertThat(objectMapper.findMixInClassFor(JobParameter.class)).isNotNull();
		assertThat(objectMapper.findMixInClassFor(JobInstance.class)).isNotNull();
		assertThat(objectMapper.findMixInClassFor(ExitStatus.class)).isNotNull();
		assertThat(objectMapper.findMixInClassFor(StepExecution.class)).isNotNull();
		assertThat(objectMapper.findMixInClassFor(ExecutionContext.class)).isNotNull();
		assertThat(objectMapper.findMixInClassFor(StepExecutionHistory.class)).isNotNull();
	}


	@Test
	void thatPrepareRestTemplateWithNullContructorValueContainsMixins() {
		final RestTemplate restTemplate = DataFlowTemplate.prepareRestTemplate(null);

		assertThat(restTemplate).isNotNull();
		assertThat(restTemplate.getErrorHandler() instanceof VndErrorResponseErrorHandler).isTrue();

		assertCorrectMixins(restTemplate);

	}

	@Test
	void thatPrepareRestTemplateWithProvidedRestTemplateContainsMixins() {
		final RestTemplate providedRestTemplate = new RestTemplate();
		final RestTemplate restTemplate = DataFlowTemplate.prepareRestTemplate(providedRestTemplate);

		assertThat(restTemplate).isNotNull();
		assertThat(providedRestTemplate == restTemplate).isTrue();
		assertThat(restTemplate.getErrorHandler() instanceof VndErrorResponseErrorHandler).isTrue();

		assertCorrectMixins(restTemplate);
	}

	@Test
	void prepareRestTemplateWithRestTemplateThatHasNoMessageConverters() {
		final RestTemplate providedRestTemplate = new RestTemplate();
		providedRestTemplate.getMessageConverters().clear();

		try {
			DataFlowTemplate.prepareRestTemplate(providedRestTemplate);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'messageConverters' must not be empty");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void prepareRestTemplateWithRestTemplateThatMissesJacksonConverter() {
		final RestTemplate providedRestTemplate = new RestTemplate();
		final Iterator<HttpMessageConverter<?>> iterator = providedRestTemplate.getMessageConverters().iterator();

		while (iterator.hasNext()) {
			if (iterator.next() instanceof MappingJackson2HttpMessageConverter) {
				iterator.remove();
			}
		}

		try {
			DataFlowTemplate.prepareRestTemplate(providedRestTemplate);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The RestTemplate does not contain a required MappingJackson2HttpMessageConverter.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void allActive() throws Exception{
		DataFlowTemplate template = getMockedDataFlowTemplate(true);

		assertThat(template.taskOperations()).isNotNull();
		assertThat(template.streamOperations()).isNotNull();
		assertThat(template.runtimeOperations()).isNotNull();
		assertThat(template.jobOperations()).isNotNull();
		assertThat(template.schedulerOperations()).isNotNull();

		testAlwaysActiveOperations(template);
	}

	@Test
	void allDeActive() throws Exception{
		DataFlowTemplate template = getMockedDataFlowTemplate(false);

		assertThat(template.taskOperations()).isNull();
		assertThat(template.streamOperations()).isNull();
		assertThat(template.runtimeOperations()).isNull();
		assertThat(template.jobOperations()).isNull();
		assertThat(template.schedulerOperations()).isNull();

		testAlwaysActiveOperations(template);
	}

	private void testAlwaysActiveOperations(DataFlowTemplate template) {
		//these operations are always active
		assertThat(template.aboutOperation()).isNotNull();
		assertThat(template.appRegistryOperations()).isNotNull();
		assertThat(template.completionOperations()).isNotNull();
	}

	private DataFlowTemplate getMockedDataFlowTemplate(boolean isLinksActive) throws Exception{
		RestTemplate restTemplate = mock(RestTemplate.class);
		RootResource rootResource = mock(RootResource.class);
		Link link = mock(Link.class);
		when(link.getHref()).thenReturn("https://whereever");
		when(rootResource.getApiRevision()).thenReturn(Version.REVISION);
		when(rootResource.getLink(any(LinkRelation.class))).thenReturn(Optional.of(link));
		when(rootResource.hasLink(any(LinkRelation.class))).thenReturn(isLinksActive);
		when(rootResource.getLink(anyString())).thenReturn(Optional.of(link));
		when(rootResource.hasLink(anyString())).thenReturn(isLinksActive);
		when(restTemplate.getForObject(any(),any())).thenReturn(rootResource);
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new MappingJackson2HttpMessageConverter());
		when(restTemplate.getMessageConverters()).thenReturn(converters);
		URI uri = new URI("foo");
		return new DataFlowTemplate(uri, restTemplate, mapper);
	}
}
