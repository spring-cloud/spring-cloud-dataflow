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
import org.springframework.cloud.dataflow.server.controller.AppRegistryController;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

/**
 * Default REST resource assembler that returns the {@link AppRegistrationResource} type.
 * @author Ilayaperumal Gopinathan
 */
public class DefaultAppRegistrationAssembler<R extends AppRegistrationResource> extends RepresentationModelAssemblerSupport<AppRegistration, R> {

	public DefaultAppRegistrationAssembler(Class<R> resourceType) {
		super(AppRegistryController.class, resourceType);
	}

	@Override
	public R toModel(AppRegistration registration) {
		return createModelWithId(String.format("%s/%s/%s", registration.getType(), registration.getName(),
				registration.getVersion()), registration);
	}

	@Override
	protected R instantiateModel(AppRegistration registration) {
		AppRegistrationResource appRegistrationResource = (registration.getVersions() == null)
				? new AppRegistrationResource(
						registration.getName(),
						registration.getType().name(),
						registration.getVersion(),
						registration.getUri().toString(),
						registration.getMetadataUri() != null ? registration.getMetadataUri().toString() : null,
						registration.isDefaultVersion()
				) : new AppRegistrationResource(
						registration.getName(),
						registration.getType().name(),
						registration.getVersion(),
						registration.getUri().toString(),
						registration.getMetadataUri() != null ? registration.getMetadataUri().toString() : null,
						registration.isDefaultVersion(),
						registration.getVersions()
				);
		return (R) appRegistrationResource;
	}
}
