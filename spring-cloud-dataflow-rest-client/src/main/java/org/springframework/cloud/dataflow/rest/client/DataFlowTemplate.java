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

package org.springframework.cloud.dataflow.rest.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.cloud.dataflow.rest.resource.RootResource;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.rest.support.jackson.ExecutionContextJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.ExitStatusJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
import org.springframework.cloud.dataflow.rest.support.jackson.JobExecutionJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.JobInstanceJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.JobParameterJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.JobParametersJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.StepExecutionHistoryJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.StepExecutionJacksonMixIn;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of DataFlowOperations delegating to sub-templates, discovered via REST
 * relations.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Glenn Renfro
 * @author Patrick Peralta
 * @author Gary Russell
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
public class DataFlowTemplate implements DataFlowOperations {

	/**
	 * A template used for http interaction.
	 */
	protected final RestTemplate restTemplate;

	/**
	 * Holds discovered URLs of the API.
	 */
	protected final Map<String, UriTemplate> resources = new HashMap<>();

	/**
	 * REST client for stream operations.
	 */
	private final StreamOperations streamOperations;

	/**
	 * REST client for task operations.
	 */
	private final TaskOperations taskOperations;

	/**
	 * REST client for job operations.
	 */
	private final JobOperations jobOperations;

	/**
	 * REST client for app registry operations.
	 */
	private final AppRegistryOperations appRegistryOperations;

	/**
	 * REST client for completion operations.
	 */
	private final CompletionOperations completionOperations;

	/**
	 * REST Client for runtime operations.
	 */
	private final RuntimeOperations runtimeOperations;

	/**
	 * REST Client for "about" operations.
	 */
	private final AboutOperations aboutOperations;


	/**
	 * REST Client for "scheduler" operations.
	 */
	private final SchedulerOperations schedulerOperations;


	/**
	 * Setup a {@link DataFlowTemplate} using the provided baseURI. Will build a
	 * {@link RestTemplate} implicitly with the required set of Jackson MixIns. For more
	 * information, please see {@link #prepareRestTemplate(RestTemplate)}.
	 * Please be aware that the created RestTemplate will use the JDK's default timeout
	 * values. Consider passing in a custom {@link RestTemplate} or, depending on your JDK
	 * implementation, set System properties such as:
	 * <ul>
	 * <li>sun.net.client.defaultConnectTimeout
	 * <li>sun.net.client.defaultReadTimeout
	 * </ul>
	 * For more information see <a href=
	 * "https://docs.oracle.com/javase/7/docs/technotes/guides/net/properties.html">this
	 * link</a>
	 *
	 * @param baseURI Must not be null
	 */
	public DataFlowTemplate(URI baseURI, ObjectMapper mapper) {
		this(baseURI, getDefaultDataflowRestTemplate(), mapper);
	}

	/**
	 * Setup a {@link DataFlowTemplate} using the provide {@link RestTemplate}. Any
	 * missing Mixins for Jackson will be added implicitly. For more information, please
	 * see {@link #prepareRestTemplate(RestTemplate)}.
	 *
	 * @param baseURI      Must not be null
	 * @param restTemplate Must not be null
	 */
	public DataFlowTemplate(URI baseURI, RestTemplate restTemplate, ObjectMapper mapper) {

		Assert.notNull(baseURI, "The provided baseURI must not be null.");
		Assert.notNull(restTemplate, "The provided restTemplate must not be null.");

		this.restTemplate = prepareRestTemplate(restTemplate);

		final RootResource resourceSupport = restTemplate.getForObject(baseURI, RootResource.class);

		if (resourceSupport != null) {
			if (resourceSupport.getApiRevision() == null) {
				throw new IllegalStateException("Incompatible version of Data Flow server detected.\n"
						+ "Follow instructions in the documentation for the version of the server you are "
						+ "using to download a compatible version of the shell.\n"
						+ "Documentation can be accessed at https://cloud.spring.io/spring-cloud-dataflow/");
			}
			String serverRevision = resourceSupport.getApiRevision().toString();
			if (!String.valueOf(Version.REVISION).equals(serverRevision)) {
				String downloadURL = getLink(resourceSupport, "dashboard").getHref() + "#about";
				throw new IllegalStateException(String.format(
						"Incompatible version of Data Flow server detected.\n"
								+ "Trying to use shell which supports revision %s, while server revision is %s. Both "
								+ "revisions should be aligned.\n"
								+ "Follow instructions at %s to download a compatible version of the shell.",
						Version.REVISION, serverRevision, downloadURL));
			}

			this.aboutOperations = new AboutTemplate(restTemplate, resourceSupport.getLink(AboutTemplate.ABOUT_REL).get());

			if (resourceSupport.hasLink(StreamTemplate.DEFINITIONS_REL)) {
				this.streamOperations = new StreamTemplate(restTemplate, resourceSupport, getVersion());
				this.runtimeOperations = new RuntimeTemplate(restTemplate, resourceSupport);
			} else {
				this.streamOperations = null;
				this.runtimeOperations = null;
			}
			if (resourceSupport.hasLink(TaskTemplate.DEFINITIONS_RELATION)) {
				if (mapper == null) {
					mapper = new ObjectMapper();
					mapper.registerModule(new Jdk8Module());
					mapper.registerModule(new Jackson2HalModule());
					mapper.registerModule(new JavaTimeModule());
					mapper.registerModule(new Jackson2DataflowModule());
				}
				this.taskOperations = new TaskTemplate(restTemplate, resourceSupport, getVersion());
				this.jobOperations = new JobTemplate(restTemplate, resourceSupport);
				if (resourceSupport.hasLink(SchedulerTemplate.SCHEDULES_RELATION)) {
					this.schedulerOperations = new SchedulerTemplate(restTemplate, resourceSupport);
				} else {
					schedulerOperations = null;
				}
			} else {
				this.taskOperations = null;
				this.jobOperations = null;
				this.schedulerOperations = null;
			}
			this.appRegistryOperations = new AppRegistryTemplate(restTemplate, resourceSupport);
			this.completionOperations = new CompletionTemplate(
					restTemplate,
					resourceSupport.getLink("completions/stream").get(),
					resourceSupport.getLink("completions/task").get()
			);
		} else {
			this.aboutOperations = null;
			this.streamOperations = null;
			this.runtimeOperations = null;
			this.taskOperations = null;
			this.jobOperations = null;
			this.appRegistryOperations = null;
			this.completionOperations = null;
			this.schedulerOperations = null;
		}
	}

	private String getVersion() {
		String version = "";
		AboutResource aboutResource = this.aboutOperations.get();
		if (aboutResource != null) {
			version = aboutResource.getVersionInfo().getCore().getVersion();
		}
		return version;
	}

	/**
	 * Will augment the provided {@link RestTemplate} with the Jackson Mixins required by
	 * Spring Cloud Data Flow, specifically:
	 * <ul>
	 * <li>{@link JobExecutionJacksonMixIn}
	 * <li>{@link JobParametersJacksonMixIn}
	 * <li>{@link JobParameterJacksonMixIn}
	 * <li>{@link JobInstanceJacksonMixIn}
	 * <li>{@link ExitStatusJacksonMixIn}
	 * <li>{@link StepExecutionJacksonMixIn}
	 * <li>{@link ExecutionContextJacksonMixIn}
	 * <li>{@link StepExecutionHistoryJacksonMixIn}
	 * </ul>
	 * <p>
	 * Furthermore, this method will also register the {@link Jackson2HalModule}
	 *
	 * @param restTemplate Can be null. Instantiates a new {@link RestTemplate} if null
	 * @return RestTemplate with the required Jackson Mixins
	 */
	public static RestTemplate prepareRestTemplate(RestTemplate restTemplate) {
		if (restTemplate == null) {
			restTemplate = new RestTemplate();
		}

		restTemplate.setErrorHandler(new VndErrorResponseErrorHandler(restTemplate.getMessageConverters()));

		boolean containsMappingJackson2HttpMessageConverter = false;

		for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
			if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
				containsMappingJackson2HttpMessageConverter = true;
				DataFlowTemplate.prepareObjectMapper(jacksonConverter.getObjectMapper());
			}
		}

		if (!containsMappingJackson2HttpMessageConverter) {
			throw new IllegalArgumentException(
					"The RestTemplate does not contain a required " + "MappingJackson2HttpMessageConverter.");
		}
		return restTemplate;
	}

	/**
	 * Mutable operation to add several required MixIns to the provided
	 * {@link ObjectMapper}.
	 *
	 * @param objectMapper Must not be null
	 * @return ObjectMapper with several mixIns applied
	 */
	public static ObjectMapper prepareObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "The objectMapper must not be null.");
		return objectMapper
				.registerModules(new Jackson2HalModule(), new Jackson2DataflowModule());
	}

	/**
	 * Invokes {@link #prepareRestTemplate(RestTemplate)}.
	 *
	 * @return RestTemplate with the required Jackson MixIns applied
	 */
	public static RestTemplate getDefaultDataflowRestTemplate() {
		return prepareRestTemplate(null);
	}

	public Link getLink(RepresentationModel<?> resourceSupport, String rel) {
		return resourceSupport.getLink(rel).orElseThrow(() ->
				new DataFlowServerException("Server did not return a link for '" + rel + "', links: '" + resourceSupport + "'")
		);
	}

	@Override
	public StreamOperations streamOperations() {
		return streamOperations;
	}

	@Override
	public TaskOperations taskOperations() {
		return taskOperations;
	}

	@Override
	public JobOperations jobOperations() {
		return jobOperations;
	}

	@Override
	public AppRegistryOperations appRegistryOperations() {
		return appRegistryOperations;
	}

	@Override
	public CompletionOperations completionOperations() {
		return completionOperations;
	}

	@Override
	public RuntimeOperations runtimeOperations() {
		return runtimeOperations;
	}

	@Override
	public AboutOperations aboutOperation() {
		return aboutOperations;
	}

	@Override
	public SchedulerOperations schedulerOperations() {
		return schedulerOperations;
	}

	/**
	 * @return The underlying RestTemplate, will never return null
	 */
	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

}
