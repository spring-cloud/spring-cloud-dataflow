/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.cloud.data.module.registry;

import java.util.List;

/**
 * A module registry is used to lookup {@link ModuleDefinition}s by name.
 * @author Mark Fisher
 * @author Gary Russell
 * @author Glenn Renfro
 * @author David Turanski
 */
public interface ModuleRegistry {

	/**
	 * Look up a module definition by name.
	 * @param name the module definition name
	 */
	ModuleDefinition findByName(String name);
}
