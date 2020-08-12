/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.web;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.h2.tools.Server;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.cloud.dataflow.rest.support.jackson.ExecutionContextJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.ISO8601DateFormatWithMilliSeconds;
import org.springframework.cloud.dataflow.rest.support.jackson.StepExecutionJacksonMixIn;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.hateoas.server.core.DefaultLinkRelationProvider;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Patrick Peralta
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author David Turanski
 */
@Configuration
@ConditionalOnWebApplication
public class WebConfiguration implements ServletContextInitializer, ApplicationListener<ContextClosedEvent> {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebConfiguration.class);

	private static final String REL_PROVIDER_BEAN_NAME = "defaultRelProvider";

	@Value("${spring.datasource.url:#{null}}")
	private String dataSourceUrl;

	private Server server = null;
	private LongTaskTimer.Sample longTaskSample;

	public Server initH2TCPServer() {
		logger.info("Starting H2 Server with URL: " + dataSourceUrl);
		try {
			this.server = Server
					.createTcpServer("-ifNotExists", "-tcp", "-tcpAllowOthers", "-tcpPort", getH2Port(dataSourceUrl))
					.start();
		}
		catch (SQLException e) {
			throw new IllegalStateException(e);
		}
		return server;
	}

	private String getH2Port(String url) {
		String[] tokens = StringUtils.tokenizeToStringArray(url, ":");
		Assert.isTrue(tokens.length >= 5, "URL not properly formatted");
		return tokens[4].substring(0, tokens[4].indexOf("/"));
	}

	@Override
	public void onStartup(ServletContext servletContext) {
		LongTaskTimer longTaskTimer = LongTaskTimer
				.builder("spring.cloud.dataflow.server").description("Spring Cloud Data Flow duration timer")
				.tags(Tags.empty()).register(Metrics.globalRegistry);
		this.longTaskSample = longTaskTimer.start();

		if (StringUtils.hasText(dataSourceUrl) && dataSourceUrl.startsWith("jdbc:h2:tcp://localhost:")) {
			logger.info("Start Embedded H2");
			initH2TCPServer();
		}
	}

	@Bean
	public HttpMessageConverters messageConverters(ObjectMapper objectMapper) {
		return new HttpMessageConverters(
				// Prevent default converters
				false,
				Arrays.<HttpMessageConverter<?>>asList(new MappingJackson2HttpMessageConverter(objectMapper),
						new ResourceHttpMessageConverter()));
	}

	@Bean
	public WebMvcConfigurer configurer() {
		return new WebMvcConfigurer() {

			@Override
			public void configurePathMatch(PathMatchConfigurer configurer) {
				configurer.setUseSuffixPatternMatch(false);
			}
		};
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer dataflowObjectMapperBuilderCustomizer() {
		return (builder) -> {
			builder.dateFormat(new ISO8601DateFormatWithMilliSeconds(TimeZone.getDefault(), Locale.getDefault(), true));
			// apply SCDF Batch Mixins to
			// ignore the JobExecution in StepExecution to prevent infinite loop.
			// https://github.com/spring-projects/spring-hateoas/issues/333
			builder.mixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
			builder.mixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
			builder.modules(new JavaTimeModule(), new Jdk8Module());
		};
	}

	@Bean
	public BeanPostProcessor relProviderOverridingBeanPostProcessor() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				// Override the LinkRelationProvider to DefaultRelProvider
				// Since DataFlow UI expects DefaultRelProvider to be used, override any
				// other instance of
				// DefaultRelProvider (EvoInflectorRelProvider for instance) with the
				// DefaultRelProvider.
				if (beanName != null && beanName.equals(REL_PROVIDER_BEAN_NAME)) {
					return new DefaultLinkRelationProvider();
				}
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String s) throws BeansException {
				return bean;
			}
		};
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		if (this.longTaskSample != null) {
			this.longTaskSample.stop();
			this.longTaskSample = null;
		}
		if (this.server != null) {
			this.server.stop();
			logger.info("Embedded H2 server stopped!");
		}
	}
}
