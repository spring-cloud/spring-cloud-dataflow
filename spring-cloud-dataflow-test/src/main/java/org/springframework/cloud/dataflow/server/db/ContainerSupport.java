/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.cloud.dataflow.server.db;

import java.util.Locale;

/**
 * Provides support for running on Mac ARM64.
 *
 * @author Chris Bono
 */
public final class ContainerSupport {

	private ContainerSupport() {
	}

	public static boolean runningOnMacArm64() {
		String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		String osArchitecture = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
		// When using Colima, the 'os.arch' property will report 'x86', therefore also look at the arch data model
		String osArchDataModel = System.getProperty("sun.arch.data.model", "unknown").toLowerCase(Locale.ROOT);
		return osName.contains("mac") && (osArchitecture.equals("aarch64") || osArchDataModel.equals("64"));
	}
}
