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

package org.springframework.cloud.dataflow.server.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.function.Consumer;

import io.micrometer.prometheus.rsocket.autoconfigure.PrometheusRSocketClientProperties;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.export.influx.InfluxProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.cloud.dataflow.core.RelaxedNames;
import org.springframework.cloud.dataflow.rest.resource.about.MonitoringDashboardType;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StringUtils;

/**
 * This post-processor helps to replicate the metrics property defined for the DataFlow server to the
 * spring.cloud.dataflow.applicationProperties.stream.* and spring.cloud.dataflow.applicationProperties.task.* as well.
 * This allows to reuse the same metrics configuration for all deployed stream applications and launched tasks.
 * <br>
 * The post-processor also automatically computes some Monitoring Dashboard properties from the server's
 * metrics properties.
 * <br>
 * Only the properties not explicitly set are updated. That means that you can explicitly set any monitoring dashboard or
 * stream/task metrics and your settings will be honored.
 *
 * @author Christian Tzolov
 * @author Chris Bono
 */
public class MetricsReplicationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final Logger logger = LoggerFactory.getLogger(MetricsReplicationEnvironmentPostProcessor.class);
	private static final String PROPERTY_SOURCE_KEY_NAME = MetricsReplicationEnvironmentPostProcessor.class.getName();
	private static final String MONITORING_PREFIX = retrievePropertyPrefix(DataflowMetricsProperties.class);
	private static final String MONITORING_DASHBOARD_PREFIX = MONITORING_PREFIX + ".dashboard";
	private static final String COMMON_APPLICATION_PREFIX = retrievePropertyPrefix(CommonApplicationProperties.class);
	private static final String COMMON_STREAM_PROPS_PREFIX = COMMON_APPLICATION_PREFIX + ".stream.";
	private static final String COMMON_TASK_PROPS_PREFIX = COMMON_APPLICATION_PREFIX + ".task.";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

		try {
			// Disable (only for this processor) the failures on unresolved placeholders.
			environment.setIgnoreUnresolvableNestedPlaceholders(true);

			Properties additionalProperties = new Properties();

			// 1. Infer the Monitoring Dashboard properties from the server's metrics configuration properties.
			this.inferMonitoringDashboardProperties(environment, additionalProperties);

			// 2. Replicates the server's metrics properties to the applicationProperties.stream
			//    and applicationProperties.task.
			if (environment.getProperty(MONITORING_PREFIX + ".property-replication", Boolean.class, true)) {
				// Callback function to handle property replication
				Consumer<String> propertyReplicator = metricsPropName -> {
					if (environment.containsProperty(metricsPropName)) {
						try {
							String serverPropValue = environment.getProperty(metricsPropName);
							ensurePropIsReplicatedExactlyOnceToCommonStreamsAndTasksProps(metricsPropName, serverPropValue,
									environment, additionalProperties);
						}
						catch (Throwable throwable) {
							logger.error("Failed with replicating {}, because of {}", metricsPropName,
									ExceptionUtils.getRootCauseMessage(throwable));
						}
					}
				};
				this.replicateServerMetricsPropertiesToStreamAndTask(environment, WavefrontProperties.class, propertyReplicator);
				this.replicateServerMetricsPropertiesToStreamAndTask(environment, InfluxProperties.class, propertyReplicator);
				this.replicateServerMetricsPropertiesToStreamAndTask(environment, PrometheusProperties.class, propertyReplicator);
				this.replicateServerMetricsPropertiesToStreamAndTask(environment, PrometheusRSocketClientProperties.class, propertyReplicator);
			}

			// This post-processor is called multiple times but sets the properties only once.
			if (!additionalProperties.isEmpty()) {
				PropertiesPropertySource propertiesPropertySource =
						new PropertiesPropertySource(PROPERTY_SOURCE_KEY_NAME, additionalProperties);
				environment.getPropertySources().addLast(propertiesPropertySource);
			}
		} finally {
			environment.setIgnoreUnresolvableNestedPlaceholders(false);
		}
	}

	private void ensurePropIsReplicatedExactlyOnceToCommonStreamsAndTasksProps(String metricsPropName, Object serverPropValue,
			Environment environment, Properties additionalProperties) {
		ensurePropIsReplicatedExactlyOnceToCommonProps(metricsPropName, serverPropValue, COMMON_STREAM_PROPS_PREFIX,
				environment, additionalProperties);
		ensurePropIsReplicatedExactlyOnceToCommonProps(metricsPropName, serverPropValue, COMMON_TASK_PROPS_PREFIX,
				environment, additionalProperties);
	}

	private void ensurePropIsReplicatedExactlyOnceToCommonProps(String metricsPropName,
			Object serverPropValue, String commonPropsPrefix, Environment environment, Properties additionalProperties) {
		// Only add if not already added explicitly
		String commonStreamPropName = commonPropsPrefix + metricsPropName;
		if (!environment.containsProperty(commonStreamPropName)) {
			logger.info("Replicate metrics property:{}={}", commonStreamPropName, serverPropValue);
			// Only add it once
			additionalProperties.putIfAbsent(commonStreamPropName, serverPropValue);
		}
	}

	/**
	 * Checks if the 'management.<meter-registry>.metrics.export.enabled' property is set to true for the specified
	 * meter registry.
	 *
	 * @param meterRegistryConfigPropsClass the SpringBoot configuration properties for the meter registry
	 * @param environment the application environment
	 * @return whether the 'management.<meter-registry>.metrics.export.enabled' property is set to true for the
	 * specified meter registry class.
	 */
	private boolean isMetricsRegistryEnabled(Class<?> meterRegistryConfigPropsClass, ConfigurableEnvironment environment) {
		String metricsPrefix = retrievePropertyPrefix(meterRegistryConfigPropsClass);
		if (!StringUtils.hasText(metricsPrefix)) {
			logger.warn("Meter registry properties class %s is not a @ConfigurationProperties".formatted(meterRegistryConfigPropsClass));
			return false;
		}
		// Some metrics props have their 'metrics.export' portion factored into nested classes (e.g. Wavefront) but
		// some metrics props still contain 'metrics.export' in their config props prefix (e.g. Influx).
		if (!metricsPrefix.endsWith(".metrics.export")) {
			metricsPrefix += ".metrics.export";
		}
		return environment.getProperty(metricsPrefix + ".enabled", Boolean.class, false);
	}

	/**
	 * Get the value of the {@code prefix} attribute of the {@link ConfigurationProperties} that the property class is
	 * annotated with.
	 * @param metricsPropertyClass property class annotated with the config properties
	 * @return return the value for the prefix of the config properties or null
	 */
	private static String retrievePropertyPrefix(Class<?> metricsPropertyClass) {
		if (metricsPropertyClass.isAnnotationPresent(ConfigurationProperties.class)) {
			ConfigurationProperties cp = metricsPropertyClass.getAnnotation(ConfigurationProperties.class);
			return StringUtils.hasText(cp.prefix()) ? cp.prefix() : cp.value();
		}
		return null;
	}

	/**
	 * Infers some Monitoring Dashboard configurations from the sever metrics configuration.
	 * @param environment Spring configuration environment.
	 * @param properties new properties.
	 */
	private void inferMonitoringDashboardProperties(ConfigurableEnvironment environment, Properties properties) {
		try {
			if (isMetricsRegistryEnabled(WavefrontProperties.class, environment)) {
				logger.info("Dashboard type:" + MonitoringDashboardType.WAVEFRONT);
				properties.setProperty(MONITORING_DASHBOARD_PREFIX + ".type", MonitoringDashboardType.WAVEFRONT.name());
				if (!environment.containsProperty(MONITORING_DASHBOARD_PREFIX + ".wavefront.source")
						&& environment.containsProperty("management.wavefront.source")) {
					properties.setProperty(MONITORING_DASHBOARD_PREFIX + ".wavefront.source",
							environment.getProperty("management.wavefront.source"));
				}
				if (!environment.containsProperty(MONITORING_DASHBOARD_PREFIX + ".url") &&
						environment.containsProperty("management.wavefront.uri")) {
					properties.setProperty(MONITORING_DASHBOARD_PREFIX + ".url",
							environment.getProperty("management.wavefront.uri"));
				}
			}
			else if (isMetricsRegistryEnabled(PrometheusProperties.class, environment)
					|| isMetricsRegistryEnabled(InfluxProperties.class, environment)) {
				logger.info("Dashboard type:" + MonitoringDashboardType.GRAFANA);
				properties.setProperty(MONITORING_DASHBOARD_PREFIX + ".type", MonitoringDashboardType.GRAFANA.name());
			}
		}
		catch (Throwable throwable) {
			logger.warn("Failed to compute the Monitoring Dashboard properties", throwable);
		}
	}

	/**
	 * When the metre registry is enabled for the provided meter property class, recursively retrieve all class fields
	 * (including the inherent and nested inner classes one) and compute metrics property candidates to check for
	 * replication. The generated properties are send to the propertyReplicator for processing.
	 *
	 * @param environment Spring configuration environment.
	 * @param propertyClass Class type for which metrics properties will be retrieved.
	 * @param propertyReplicator Callback function that will replicate the computed property if it is set in the
	 *                           server configuration.
	 */
	private void replicateServerMetricsPropertiesToStreamAndTask(ConfigurableEnvironment environment,
			Class<?> propertyClass, Consumer<String> propertyReplicator) {
		try {
			if (isMetricsRegistryEnabled(propertyClass, environment)) {
				// Note: For some meter registries, the management.<meter-registry>.metrics.export.enabled property
				// is not defined as explicit field. We need to handle it explicitly.
				propertyReplicator.accept(retrievePropertyPrefix(propertyClass) + ".metrics.export.enabled");
				traversePropertyClassFields(propertyClass, propertyReplicator);
			}
		}
		catch (Throwable throwable) {
			logger.warn("Failed to replicate server Metrics for " + propertyClass.getName(), throwable);
		}
	}

	/**
	 * Converts the class fields into metrics property candidates and handles them to the replication handler
	 * to process. The metrics prefix is retrieved from the {@link ConfigurationProperties} annotation.
	 * Drops the non-annotated classes.
	 * <br>
	 * The traversePropertyClassFields iterates and repeats the computation over the class's parent
	 * classes when available.
	 *
	 * @param metricsPropertyClass Class type for which metrics properties will be retrieved.
	 * @param metricsReplicationHandler Handler to process the generated metrics properties candidates.
	 */
	private void traversePropertyClassFields(Class<?> metricsPropertyClass, Consumer<String> metricsReplicationHandler) {
		String metricsPrefix = retrievePropertyPrefix(metricsPropertyClass);
		if (StringUtils.hasText(metricsPrefix)) {
			do {
				traverseClassFieldsRecursively(metricsPropertyClass, metricsPrefix, metricsReplicationHandler);
				// traverse the parent class if not Object.
				metricsPropertyClass = metricsPropertyClass.getSuperclass();
			} while (metricsPropertyClass != null && !metricsPropertyClass.isAssignableFrom(Object.class));
		}
	}

	/**
	 * Iterate over the fields of the provided class. For non-inner class fields generate a metrics property candidate
	 * and pass it to the metrics replication handler for processing. For the inner-class fields extend the
	 * prefix with the name of the field and call traverseClassFieldsRecursively recursively.
	 * <br/>
	 * Use the RelaxedNames.camelCaseToHyphenLower utility to convert the field names into property keys.
	 *
	 * @param metricsPropertyClass Class to be processed.
	 * @param metricsPrefix  Metrics prefix to prefix the retrieved fields before pass them to the handler.
	 * @param metricsReplicationHandler callback function that will replicate the computed property if it is set in
	 *                                  the server configuration.
	 */
	private void traverseClassFieldsRecursively(Class<?> metricsPropertyClass, String metricsPrefix,
			Consumer<String> metricsReplicationHandler) {
		for (Field field : metricsPropertyClass.getDeclaredFields()) {
			var isStaticMemberClass = field.getType().isMemberClass() && Modifier.isStatic(field.getType().getModifiers());
			if (isStaticMemberClass && !field.getType().isEnum()) {
				// traverse the inner class recursively.
				String innerMetricsPrefix = metricsPrefix + "."
						+ RelaxedNames.camelCaseToHyphenLower(field.getName());
				traverseClassFieldsRecursively(field.getType(), innerMetricsPrefix, metricsReplicationHandler);
			}
			else {
				metricsReplicationHandler
					.accept(metricsPrefix + "." + RelaxedNames.camelCaseToHyphenLower(field.getName()));
			}
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
