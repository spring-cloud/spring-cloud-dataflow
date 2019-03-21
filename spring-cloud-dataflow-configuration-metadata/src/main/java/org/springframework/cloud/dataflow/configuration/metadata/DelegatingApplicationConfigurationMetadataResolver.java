/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.core.io.Resource;

/**
 * An {@link ApplicationConfigurationMetadataResolver} that tries several delegates in turn,
 * returning the results of the first one that {@link ApplicationConfigurationMetadataResolver#supports(Resource)}
 * an app. Returns an empty list of properties as a last resort.
 *
 * @author Eric Bottard
 */
public class DelegatingApplicationConfigurationMetadataResolver extends ApplicationConfigurationMetadataResolver {

	private List<ApplicationConfigurationMetadataResolver> delegates = new ArrayList<>();

	public DelegatingApplicationConfigurationMetadataResolver(ApplicationConfigurationMetadataResolver... delegates) {
		this.delegates = Arrays.asList(delegates);
	}

	@Override
	public boolean supports(Resource app) {
		return true; // We're going to return an empty list as a last resort anyway
	}

	@Override
	public List<ConfigurationMetadataProperty> listProperties(Resource app, boolean exhaustive) {
		for (ApplicationConfigurationMetadataResolver delegate : delegates) {
			if (delegate.supports(app)) {
				return delegate.listProperties(app, exhaustive);
			}
		}
		return Collections.emptyList();
	}

	@Override
	public URLClassLoader createAppClassLoader(Resource app) {
		for (ApplicationConfigurationMetadataResolver delegate : delegates) {
			if (delegate.supports(app)) {
				return delegate.createAppClassLoader(app);
			}
		}
		return super.createAppClassLoader(app);
	}
}
