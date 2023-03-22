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

public class DockerComposeRunOption {

	private List<String> options;

	public DockerComposeRunOption(List<String> options) {
		this.options = options;
	}

	public List<String> options() {
		return options;
	}

    public static DockerComposeRunOption options(String... options) {
        return DockerComposeRunOption.of(Arrays.asList(options));
    }

	private static DockerComposeRunOption of(List<String> asList) {
		return new DockerComposeRunOption(asList);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
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
			DockerComposeRunOption other = (DockerComposeRunOption) obj;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

}
