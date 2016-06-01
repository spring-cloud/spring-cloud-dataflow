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

import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.h2.tools.Server;
import org.slf4j.LoggerFactory;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.SimpleJobServiceFactoryBean;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.repository.redis.RedisMetricRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.completion.RecoveryStrategy;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.rest.job.support.ISO8601DateFormatWithMilliSeconds;
import org.springframework.cloud.dataflow.server.completion.TapOnDestinationRecoveryStrategy;
import org.springframework.cloud.dataflow.server.config.security.SecurityConfiguration;
import org.springframework.cloud.dataflow.server.job.TaskExplorerFactoryBean;
import org.springframework.cloud.dataflow.server.job.support.ExecutionContextJacksonMixIn;
import org.springframework.cloud.dataflow.server.job.support.StepExecutionJacksonMixIn;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.RdbmsDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.RdbmsStreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.RedisDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.RedisStreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepositoryFactoryBean;
import org.springframework.cloud.dataflow.server.repository.support.DefinitionRepositoryInitializer;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskJobService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.stream.app.metrics.FieldValueCounterRepository;
import org.springframework.cloud.stream.app.metrics.redis.RedisFieldValueCounterRepository;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.core.DefaultRelProvider;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

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
 * @author Michael Minella
 * @author Gunnar Hillert
 */
@Configuration
@EnableHypermediaSupport(type = HAL)
@EnableSpringDataWebSupport
@Import(CompletionConfiguration.class)
@ComponentScan(basePackageClasses = {StreamDefinitionRepository.class, SecurityConfiguration.class})
@EnableAutoConfiguration
@EnableConfigurationProperties(BatchProperties.class)
public class DataFlowServerConfiguration {

	protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(DataFlowServerConfiguration.class);

	private static final String REL_PROVIDER_BEAN_NAME = "defaultRelProvider";

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
	public BeanPostProcessor relProviderOverridingBeanPostProcessor() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				// Override the RelProvider to DefaultRelProvider
				// Since DataFlow UI expects DefaultRelProvider to be used, override any other instance of
				// DefaultRelProvider (EvoInflectorRelProvider for instance) with the DefaultRelProvider.
				if (beanName.equals(REL_PROVIDER_BEAN_NAME)) {
					return new DefaultRelProvider();
				}
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String s) throws BeansException {
				return bean;
			}
		};
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("#{'${serverStore:redis}'.equals('db')}")
	public StreamDefinitionRepository rdbmsStreamDefinitionRepository(DataSource dataSource) {
		return new RdbmsStreamDefinitionRepository(dataSource);
	}

	@Bean
	@ConditionalOnExpression("#{'${serverStore:redis}'.equals('db')}")
	public DeploymentIdRepository rdbmsDeploymentIdRepository(DataSource dataSource) {
		return new RdbmsDeploymentIdRepository(dataSource);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("#{'${serverStore:redis}'.equals('redis')}")
	public StreamDefinitionRepository streamDefinitionRepository(RedisConnectionFactory redisConnectionFactory) {
		return new RedisStreamDefinitionRepository("stream-definitions", redisConnectionFactory);
	}


	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("#{'${serverStore:redis}'.equals('redis')}")
	public DeploymentIdRepository deploymentIdRepository(RedisConnectionFactory redisConnectionFactory) {
		return new RedisDeploymentIdRepository("deployment-ids", redisConnectionFactory);
	}

	@Configuration
	@ConditionalOnWebApplication
	public static class ServerWebConfiguration {

		private static final String SPRING_HATEOAS_OBJECT_MAPPER = "_halObjectMapper";

		/**
		 * Obtains the Spring Hateos Object Mapper so that we can apply SCDF Batch Mixins
		 * to ignore the JobExecution in StepExecution to prevent infinite loop.
		 * {@see https://github.com/spring-projects/spring-hateoas/issues/333}
		 */
		@Autowired
		@Qualifier(SPRING_HATEOAS_OBJECT_MAPPER)
		private ObjectMapper springHateoasObjectMapper;

		@Bean
		@Primary
		public ObjectMapper objectMapper() {
			ObjectMapper objectMapper = springHateoasObjectMapper;
			setupObjectMapper(objectMapper);
			return objectMapper;
		}

		@Bean
		public HttpMessageConverters messageConverters() {
			final ObjectMapper objectMapper = new ObjectMapper();
			setupObjectMapper(objectMapper);
			return new HttpMessageConverters(
					// Prevent default converters
					false,
					// Have Jackson2 converter as the sole converter
					Arrays.<HttpMessageConverter<?>>asList(new MappingJackson2HttpMessageConverter(objectMapper)));
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

		private void setupObjectMapper(ObjectMapper objectMapper) {
			objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
			objectMapper.setDateFormat(new ISO8601DateFormatWithMilliSeconds());
			objectMapper.addMixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
			objectMapper.addMixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
		}
	}

	@Bean
	@ConditionalOnMissingBean(TapOnDestinationRecoveryStrategy.class)
	public RecoveryStrategy<?> tapOnDestinationExpansionStrategy(StreamCompletionProvider streamCompletionProvider,
			StreamDefinitionRepository streamDefinitionRepository) {
		RecoveryStrategy<?> recoveryStrategy = new TapOnDestinationRecoveryStrategy(streamDefinitionRepository);
		streamCompletionProvider.addCompletionRecoveryStrategy(recoveryStrategy);
		return recoveryStrategy;
	}

	@Bean
	public TaskExplorerFactoryBean taskExplorerFactoryBean(DataSource dataSource) {
		return new TaskExplorerFactoryBean(dataSource);
	}

	@Bean
	public TaskService taskService(TaskDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, UriRegistry registry, DelegatingResourceLoader resourceLoader,
			TaskLauncher taskLauncher) {
		return new DefaultTaskService(repository, deploymentIdRepository, registry, resourceLoader, taskLauncher);
	}

	@Bean
	public TaskJobService taskJobExecutionRepository(JobService service,
			TaskExplorer taskExplorer, TaskDefinitionRepository taskDefinitionRepository, TaskService taskService) {
		return new DefaultTaskJobService(service, taskExplorer, taskDefinitionRepository, taskService);
	}

	@Bean
	public SimpleJobServiceFactoryBean simpleJobServiceFactoryBean(DataSource dataSource,
			JobRepositoryFactoryBean repositoryFactoryBean) throws Exception {
		SimpleJobServiceFactoryBean factoryBean = new SimpleJobServiceFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setJobRepository(repositoryFactoryBean.getObject());
		factoryBean.setJobLocator(new MapJobRegistry());
		factoryBean.setJobLauncher(new SimpleJobLauncher());
		factoryBean.setDataSource(dataSource);
		return factoryBean;
	}

	@Bean
	public JobExplorerFactoryBean jobExplorerFactoryBean(DataSource dataSource) {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(dataSource);
		return jobExplorerFactoryBean;
	}

	@Configuration
	@ConditionalOnExpression("#{'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') && '${spring.datasource.url:}'.contains('/mem:')}")
	public static class H2ServerConfiguration {

		@Bean
		public JobRepositoryFactoryBean jobRepositoryFactoryBeanForServer(DataSource dataSource,
				Server server, DataSourceTransactionManager dataSourceTransactionManager) {
			JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
			repositoryFactoryBean.setDataSource(dataSource);
			repositoryFactoryBean.setTransactionManager(dataSourceTransactionManager);
			return repositoryFactoryBean;
		}

		@Bean
		public DataSourceTransactionManager transactionManagerForServer(DataSource dataSource, Server server) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		public BatchDatabaseInitializer batchRepositoryInitializerForDefaultDBForServer(DataSource dataSource, Server server) {
			return new BatchDatabaseInitializer();
		}

		@Bean
		public TaskRepositoryInitializer taskRepositoryInitializerForDefaultDB(DataSource dataSource, Server server) {
			TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
			taskRepositoryInitializer.setDataSource(dataSource);
			return taskRepositoryInitializer;
		}

		@Bean
		public DefinitionRepositoryInitializer definitionRepositoryInitializerForDefaultDB(DataSource dataSource, Server server) {
			DefinitionRepositoryInitializer definitionRepositoryInitializer = new DefinitionRepositoryInitializer();
			definitionRepositoryInitializer.setDataSource(dataSource);
			return definitionRepositoryInitializer;
		}

		@Bean
		@ConditionalOnMissingBean
		public TaskDefinitionRepository taskDefinitionRepository(DataSource dataSource, Server server) throws Exception{
			return (new TaskDefinitionRepositoryFactoryBean(dataSource)).getObject();
		}
	}

	@Configuration
	@ConditionalOnExpression("#{!'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') && !'${spring.datasource.url:}'.contains('/mem:')}")
	public static class NoH2ServerConfiguration {

		@Bean
		public JobRepositoryFactoryBean jobRepositoryFactoryBean(DataSource dataSource,
				DataSourceTransactionManager dataSourceTransactionManager) {
			JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
			repositoryFactoryBean.setDataSource(dataSource);
			repositoryFactoryBean.setTransactionManager(dataSourceTransactionManager);
			return repositoryFactoryBean;
		}

		@Bean
		public DataSourceTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		public BatchDatabaseInitializer batchRepositoryInitializerForDefaultDB(DataSource dataSource) {
			return new BatchDatabaseInitializer();
		}

		@Bean
		public TaskRepositoryInitializer taskRepositoryInitializerForDB(DataSource dataSource) {
			TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
			taskRepositoryInitializer.setDataSource(dataSource);
			return taskRepositoryInitializer;
		}

		@Bean
		public DefinitionRepositoryInitializer definitionRepositoryInitializerForDefaultDB(DataSource dataSource) {
			DefinitionRepositoryInitializer definitionRepositoryInitializer = new DefinitionRepositoryInitializer();
			definitionRepositoryInitializer.setDataSource(dataSource);
			return definitionRepositoryInitializer;
		}

		@Bean
		@ConditionalOnMissingBean
		public TaskDefinitionRepository taskDefinitionRepository(DataSource dataSource) throws Exception{
			return (new TaskDefinitionRepositoryFactoryBean(dataSource)).getObject();
		}
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

	private String getH2Port(String url) {
		String[] tokens = StringUtils.tokenizeToStringArray(url, ":");
		Assert.isTrue(tokens.length >= 5, "URL not properly formatted");
		return tokens[4].substring(0, tokens[4].indexOf("/"));
	}

}
