/*
 * Copyright 2015-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller.assembler;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.hateoas.server.RepresentationModelAssembler;

/**
 * The interface that returns the {@link RepresentationModelAssembler} for any type which extends
 * {@link AppRegistrationResource}.
 *
 * @author Ilayaperumal Gopinathan
 */
public interface AppRegistrationAssemblerProvider<R extends AppRegistrationResource> {

	/**
	 * Get the {@link AppRegistration} assembler.
	 * @return the app registration assembler
	 */
	RepresentationModelAssembler<AppRegistration, R> getAppRegistrationAssembler();

}
