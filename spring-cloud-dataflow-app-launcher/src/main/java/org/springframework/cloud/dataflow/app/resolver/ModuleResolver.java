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

package org.springframework.cloud.dataflow.app.resolver;

import org.springframework.core.io.Resource;

/**
 * Interface used to return a {@link Resource} that provides access to a module
 * uber-jar based on its Maven coordinates.
 *
 * @author David Turanski
 * @author Marius Bogoevici
 * @author Eric Bottard
 */
public interface ModuleResolver {

	/**
	 * Retrieve a resource given its coordinates.
	 *
	 * @param coordinates the coordinates of a resource
	 * @return the resource
	 */
	Resource resolve(Coordinates coordinates);

	/**
	 * Retrieve a set of resources given their coordinates, along with additional dependencies.
	 * Exclusion rules patterns (conforming to {@link org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter}
	 * can be provided as well.
	 *
	 * @param root the coordinates of the main resource
	 * @param includes a list of coordinates to include along the main resource
	 * @param excludePatterns a list of exclusion patterns
	 * @see org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter
	 * @return the main resource and the additional dependencies
	 */
	Resource[] resolve(Coordinates root, Coordinates[] includes, String[] excludePatterns);

}
