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

package org.springframework.cloud.dataflow.completion;

import org.springframework.cloud.dataflow.core.ModuleCoordinates;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.stream.module.resolver.Coordinates;
import org.springframework.util.Assert;

/**
 * Created by ericbottard on 02/10/15.
 */
public class CompletionUtils {

	public static ModuleType inferType(ModuleDefinition moduleDefinition, StreamDefinition streamDefinition) {
		int index = streamDefinition.getModuleDefinitions().indexOf(moduleDefinition);
		Assert.isTrue(index >= 0, "Given moduleDefinition is not part of the streamDefinition");
		int size = streamDefinition.getModuleDefinitions().size();
		return index == 0 ? ModuleType.source : (index == size - 1 ? ModuleType.sink : ModuleType.processor);
	}

	public static Coordinates adapt(ModuleCoordinates coordinates) {
		return new Coordinates(coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getExtension(), "exec", coordinates.getVersion());
	}
}
