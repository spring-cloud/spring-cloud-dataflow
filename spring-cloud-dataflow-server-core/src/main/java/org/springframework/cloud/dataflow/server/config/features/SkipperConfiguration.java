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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Skipper related configurations.
 *
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@ConditionalOnProperty(prefix = FeaturesProperties.FEATURES_PREFIX, name = FeaturesProperties.SKIPPER_ENABLED)
public class SkipperConfiguration {

	private static Log logger = LogFactory.getLog(SkipperConfiguration.class);
}
