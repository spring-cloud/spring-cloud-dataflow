package org.springframework.cloud.dataflow.rest.client.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.dsl.Stream;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamBuilder;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class to provide default beans for {@link DataFlowOperations} and {@link org.springframework.cloud.dataflow.rest.client.dsl.StreamBuilder} instances.
 * @author Vinicius Carvalho
 */
@Configuration
@EnableConfigurationProperties(DataFlowClientProperties.class)
@ConditionalOnProperty(prefix = DataFlowPropertyKeys.PREFIX + "client", name = "enableDsl")
public class DataFlowClientAutoConfiguration {

	@Autowired
	private DataFlowClientProperties properties;

	@Autowired(required = false)
	private RestTemplate restTemplate;

	@Bean
	@ConditionalOnMissingBean(DataFlowOperations.class)
	public DataFlowOperations dataFlowOperations() throws Exception{
		RestTemplate template = DataFlowTemplate.prepareRestTemplate(restTemplate);
		final HttpClientConfigurer httpClientConfigurer = HttpClientConfigurer.create()
				.targetHost(new URI(properties.getTarget()))
				.skipTlsCertificateVerification(properties.isSkipSslValidation());
		if(!StringUtils.isEmpty(properties.getSecurity().getUsername()) &&
				!StringUtils.isEmpty(properties.getSecurity().getPassword())){
			httpClientConfigurer.basicAuthCredentials(properties.getSecurity().getUsername(), properties.getSecurity().getPassword());
			template.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());
		}
		return new DataFlowTemplate(new URI(properties.getTarget()), template);
	}

	@Bean
	@ConditionalOnMissingBean(StreamBuilder.class)
	public StreamBuilder streamBuilder(DataFlowOperations dataFlowOperations){
		return Stream.builder(dataFlowOperations);
	}

}
