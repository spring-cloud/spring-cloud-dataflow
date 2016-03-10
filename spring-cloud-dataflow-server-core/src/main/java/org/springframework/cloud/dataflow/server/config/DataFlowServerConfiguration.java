/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.config;

import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.repository.redis.RedisMetricRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.cloud.dataflow.artifact.registry.RedisArtifactRegistry;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.completion.RecoveryStrategy;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.server.completion.TapOnDestinationRecoveryStrategy;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.repository.InMemoryStreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDatabaseInitializer;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.stream.module.metrics.FieldValueCounterRepository;
import org.springframework.cloud.stream.module.metrics.RedisFieldValueCounterRepository;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.JdbcTaskExplorerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Configuration for the Data Flow Server application context. This includes support
 * for the REST API framework configuration.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Patrick Peralta
 * @author Thomas Risberg
 * @author Janne Valkealahti
 * @author Glenn Renfro
 * @author Josh Long
 */
@Configuration
@EnableHypermediaSupport(type = HAL)
@EnableSpringDataWebSupport
@Import(CompletionConfiguration.class)
@ComponentScan(basePackageClasses = {
		StreamDefinitionController.class,
		StreamDefinitionRepository.class
})
@EnableAutoConfiguration(exclude = OAuth2AutoConfiguration.class)
public class DataFlowServerConfiguration {

	protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(DataFlowServerConfiguration.class);

	@Value("${spring.datasource.url:#{null}}")
	private String dataSourceUrl;

	@Bean
	@ConditionalOnMissingBean
	public MetricRepository metricRepository(RedisConnectionFactory redisConnectionFactory) {
		return new RedisMetricRepository(redisConnectionFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public FieldValueCounterRepository fieldValueCounterReader(RedisConnectionFactory redisConnectionFactory) {
		return new RedisFieldValueCounterRepository(redisConnectionFactory, new RetryTemplate());
	}

	@Bean
	@ConditionalOnMissingBean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return new InMemoryStreamDefinitionRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskDefinitionRepository taskDefinitionRepository() {
		return new InMemoryTaskDefinitionRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public ArtifactRegistry artifactRegistry(RedisConnectionFactory redisConnectionFactory) {
		return new RedisArtifactRegistry(redisConnectionFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public ArtifactRegistryPopulator artifactRegistryPopulator(ArtifactRegistry artifactRegistry) {
		return new ArtifactRegistryPopulator(artifactRegistry);
	}

	@Configuration
	@ConditionalOnWebApplication
	public static class ServerWebConfiguration {

		@Bean
		public HttpMessageConverters messageConverters() {
			return new HttpMessageConverters(
					// Prevent default converters
					false,
					// Have Jackson2 converter as the sole converter
					Arrays.<HttpMessageConverter<?>>asList(new MappingJackson2HttpMessageConverter()));
		}

		@Bean
		public WebMvcConfigurer configurer() {
			return new WebMvcConfigurerAdapter() {

				@Override
				public void configurePathMatch(PathMatchConfigurer configurer) {
					configurer.setUseSuffixPatternMatch(false);
				}
			};
		}
	}

	@Bean
	@ConditionalOnMissingBean(TapOnDestinationRecoveryStrategy.class)
	public RecoveryStrategy<?> tapOnDestinationExpansionStrategy(StreamCompletionProvider streamCompletionProvider) {
		RecoveryStrategy<?> recoveryStrategy = new TapOnDestinationRecoveryStrategy(streamDefinitionRepository());
		streamCompletionProvider.addCompletionRecoveryStrategy(recoveryStrategy);
		return recoveryStrategy;
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskExplorer taskExplorer(DataSource dataSource) {
		JdbcTaskExplorerFactoryBean factoryBean = new JdbcTaskExplorerFactoryBean(dataSource);
		return factoryBean.getObject();
	}

	@Bean(destroyMethod = "stop")
	@ConditionalOnExpression("#{'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') && '${spring.datasource.url:}'.contains('/mem:')}")
	public Server initH2TCPServer() {
		Server server = null;
		logger.info("Starting H2 Server with URL: " + dataSourceUrl);
		try {
			server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort",
					getH2Port(dataSourceUrl)).start();
		}
		catch (SQLException e) {
			throw new IllegalStateException(e);
		}
		return server;
	}

	@Bean
	@ConditionalOnExpression("#{'!${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') && !'${spring.datasource.url:}'.contains('/mem:')}")
	public TaskDatabaseInitializer taskDatabaseInitializerForDB(DataSource dataSource) {
		return new TaskDatabaseInitializer(dataSource);
	}

	@Bean
	@ConditionalOnExpression("#{'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') && '${spring.datasource.url:}'.contains('/mem:')}")
	public TaskDatabaseInitializer taskDatabaseInitializerForDefaultDB(DataSource dataSource, Server server) {
		return new TaskDatabaseInitializer(dataSource);
	}

	private String getH2Port(String url) {
		String[] tokens = StringUtils.tokenizeToStringArray(url, ":");
		Assert.isTrue(tokens.length >= 5, "URL not properly formatted");
		return tokens[4].substring(0, tokens[4].indexOf("/"));
	}
}
