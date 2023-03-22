/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose.execution;

import java.util.Arrays;
import java.util.List;

public class DockerComposeRunArgument {

	private List<String> arguments;

	public DockerComposeRunArgument(List<String> arguments) {
		this.arguments = arguments;
	}

    public List<String> arguments() {
    	return arguments;
    }

    public static DockerComposeRunArgument arguments(String... arguments) {
        return DockerComposeRunArgument.of(Arrays.asList(arguments));
    }

	private static DockerComposeRunArgument of(List<String> asList) {
		return new DockerComposeRunArgument(asList);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arguments == null) ? 0 : arguments.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
			DockerComposeRunArgument other = (DockerComposeRunArgument) obj;
		if (arguments == null) {
			if (other.arguments != null)
				return false;
		} else if (!arguments.equals(other.arguments))
			return false;
		return true;
	}
}
