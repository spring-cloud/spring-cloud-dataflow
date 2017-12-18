/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for skipper server.
 *
 * @author Janne Valkealahti
 */
@Configuration
@ConditionalOnBean(EnableSkipperServerConfiguration.Marker.class)
@Import({SkipperServerConfiguration.class, SkipperServerPlatformConfiguration.class})
@ConditionalOnProperty(prefix = "spring.cloud.skipper.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SkipperServerAutoConfiguration {
}
