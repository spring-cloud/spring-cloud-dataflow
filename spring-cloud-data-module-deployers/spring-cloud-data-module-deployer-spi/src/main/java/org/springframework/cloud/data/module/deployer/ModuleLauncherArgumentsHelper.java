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

package org.springframework.cloud.data.module.deployer;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to generate qualified environmental properties from a
 * {@link org.springframework.cloud.data.core.ModuleDeploymentRequest}, in a format that is expected by
 * ModuleLauncher and ModuleLauncherProperties.
 *
 * @author Eric Bottard
 */
public class ModuleLauncherArgumentsHelper {

	private ModuleLauncherArgumentsHelper() {
		
	}

	/**
	 * Return a qualified version of the given arguments, suitable for passing to the ModuleLauncher, in the context
	 * of launching a module appearing at position {@literal index} in the list of modules to be launched.
	 *
	 * <p>No other operation is performed on the keys. Callers may want to <i>e.g.</i> turn the result to uppercase and
	 * replace dots with underscores if the arguments are to be passed <i>via</i> environment variables.
	 * </p>
	 *
	 * @param index position of the module the arguments pertain to
	 * @param unqualified a raw map of key value pairs to be passed as arguments
	 * @return a map of key value pairs where each ket has been transformed, in "dot" form
	 */
	public static Map<String, String> qualifyArgs(int index, Map<String, String> unqualified) {
		Map<String, String> result = new HashMap<>(unqualified.size());
		for (Map.Entry<String, String> entry : unqualified.entrySet()) {
			result.put(String.format("args.%d.%s", index, entry.getKey()), entry.getValue());
		}
		return result;
	}
}
