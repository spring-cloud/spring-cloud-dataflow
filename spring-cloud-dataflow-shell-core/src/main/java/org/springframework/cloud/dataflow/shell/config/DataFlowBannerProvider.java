/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.BannerProvider;
import org.springframework.shell.support.util.FileUtils;
import org.springframework.stereotype.Component;

/**
 * Provides the Spring Cloud Data Flow specific {@link BannerProvider}.
 *
 * @author Gunnar Hillert
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 * @since 1.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataFlowBannerProvider implements BannerProvider {

	private static final String WELCOME = "Welcome to the Spring Cloud Data Flow shell. For assistance hit TAB or "
			+ "type \"help\".";

	@Override
	public String getProviderName() {
		return "dataflow";
	}

	@Override
	public String getBanner() {
		return FileUtils.readBanner(DataFlowBannerProvider.class, "/dataflow-banner.txt") + "\n" + getVersion() + "\n";
	}

	/**
	 * Returns the version information as found in the manifest file (set during release).
	 */
	@Override
	public String getVersion() {
		Package pkg = DataFlowBannerProvider.class.getPackage();
		String version = null;
		if (pkg != null) {
			version = pkg.getImplementationVersion();
		}
		return (version != null ? version : "Unknown Version");
	}

	@Override
	public String getWelcomeMessage() {
		return WELCOME;
	}

}
