/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.server.support.CannotDetermineApplicationTypeException;
import org.springframework.cloud.deployer.spi.core.AppDefinition;

/**
 * Utility class holding helper methods used by the server core.
 *
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
public class DataFlowServerUtil {

	/**
	 * Return the {@link ApplicationType} for a {@link AppDefinition} in the context
	 * of a defined stream.
	 *
	 * @param appDefinition the app for which to determine the type
	 * @throws CannotDetermineApplicationTypeException Thrown if the {@link ApplicationType} cannot be determined
	 * @return {@link ApplicationType} for the given app
	 */
	public static ApplicationType determineApplicationType(StreamAppDefinition appDefinition) {
		// Parser has already taken care of source/sink destinations, etc
		boolean hasOutput = appDefinition.getProperties().containsKey(BindingPropertyKeys.OUTPUT_DESTINATION);
		boolean hasInput = appDefinition.getProperties().containsKey(BindingPropertyKeys.INPUT_DESTINATION);
		if (hasInput && hasOutput) {
			return ApplicationType.processor;
		}
		else if (hasInput) {
			return ApplicationType.sink;
		}
		else if (hasOutput) {
			return ApplicationType.source;
		}
		else {
			throw new CannotDetermineApplicationTypeException(appDefinition.getName() + " had neither input nor output set");
		}
	}
}
