/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner.configuration;

import io.micrometer.prometheus.rsocket.autoconfigure.PrometheusRSocketClientAutoConfiguration;

import org.springframework.boot.actuate.autoconfigure.metrics.export.influx.InfluxMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.wavefront.WavefrontMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 */
@Configuration
@EnableAutoConfiguration(exclude = { DataFlowClientAutoConfiguration.class,
		WavefrontMetricsExportAutoConfiguration.class, PrometheusMetricsExportAutoConfiguration.class,
		PrometheusRSocketClientAutoConfiguration.class, InfluxMetricsExportAutoConfiguration.class })
public class DataFlowTestConfiguration {

	@Bean
	public TaskOperations taskOperations() {
		return mock(TaskOperations.class);
	}

}
