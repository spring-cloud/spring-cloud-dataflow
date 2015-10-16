/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.admin.config;

import org.springframework.cloud.dataflow.module.deployer.kubernetes.KubernetesModuleDeployerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Configuration which creates deployers that deploy on Google Kubernetes.
 * Can be used either when running <i>in</i> Kubernetes, or <i>targeting</i> Kubernetes.
 *
 * @author Florian Rosenberg
 */
@Profile("kubernetes")
@Configuration
@Import(KubernetesModuleDeployerConfiguration.class)
class KubernetesConfiguration {

}