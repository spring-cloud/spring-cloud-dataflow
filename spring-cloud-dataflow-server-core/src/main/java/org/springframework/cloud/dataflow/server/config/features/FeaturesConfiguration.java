/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.features;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.RdbmsDeploymentIdRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class that conditionally imports stream, task and anaytics configuration classes based on the
 * features that are enabled/disabled.
 *
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@Import({AnalyticsConfiguration.class, StreamConfiguration.class, TaskConfiguration.class})
public class FeaturesConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("#{'${spring.cloud.dataflow.features.streams-enabled:true}'.equalsIgnoreCase('true') || " +
            "'${spring.cloud.dataflow.features.tasks-enabled:true}'.equalsIgnoreCase('true') }")
    public DeploymentIdRepository deploymentIdRepository(DataSource dataSource) {
        return new RdbmsDeploymentIdRepository(dataSource);
    }
}
