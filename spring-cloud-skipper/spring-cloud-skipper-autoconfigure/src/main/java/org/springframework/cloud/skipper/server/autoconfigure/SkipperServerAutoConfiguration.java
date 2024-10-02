/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.server.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.skipper.server.config.EnableSkipperServerConfiguration;
import org.springframework.cloud.skipper.server.config.SkipperServerConfiguration;
import org.springframework.cloud.skipper.server.config.SkipperServerPlatformConfiguration;
import org.springframework.cloud.skipper.server.config.SpringDataRestConfiguration;
import org.springframework.cloud.skipper.server.config.security.SkipperOAuthSecurityConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for skipper server.
 *
 * @author Janne Valkealahti
 */
@AutoConfiguration
@ConditionalOnBean(EnableSkipperServerConfiguration.Marker.class)
@AutoConfigureBefore({ErrorMvcAutoConfiguration.class, CommonSecurityAutoConfiguration.class})
@Import({SkipperServerConfiguration.class, SkipperServerPlatformConfiguration.class,
		SpringDataRestConfiguration.class, SkipperOAuthSecurityConfiguration.class})
@ConditionalOnProperty(prefix = "spring.cloud.skipper.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SkipperServerAutoConfiguration {
}
