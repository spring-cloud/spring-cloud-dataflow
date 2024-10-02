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
package org.springframework.cloud.dataflow.common.test.docker.compose.connection;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;

public class ContainerName {

	private String rawName;
	private String semanticName;

    public ContainerName(String rawName, String semanticName) {
		this.rawName = rawName;
		this.semanticName = semanticName;
	}

	public String rawName() {
		return rawName;
	}

    public String semanticName() {
    	return semanticName;
    }

    @Override
    public String toString() {
        return semanticName();
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rawName == null) ? 0 : rawName.hashCode());
		result = prime * result + ((semanticName == null) ? 0 : semanticName.hashCode());
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
        ContainerName other = (ContainerName) obj;
		if (rawName == null) {
			if (other.rawName != null)
				return false;
		} else if (!rawName.equals(other.rawName))
			return false;
		if (semanticName == null) {
			if (other.semanticName != null)
				return false;
		} else if (!semanticName.equals(other.semanticName))
			return false;
		return true;
	}

    public static ContainerName fromPsLine(String psLine) {
        String[] lineComponents = psLine.split(" ");
        String rawName = lineComponents[0];

        if (probablyCustomName(rawName)) {
            return ContainerName.builder()
                .rawName(rawName)
                .semanticName(rawName)
                .build();
        }

        String semanticName = withoutDirectory(withoutScaleNumber(rawName));
        return ContainerName.builder()
                .rawName(rawName)
                .semanticName(semanticName)
                .build();
    }

    private static boolean probablyCustomName(String rawName) {
        return !(rawName.split("_").length >= 3);
    }

    private static String withoutDirectory(String rawName) {
        return Arrays.stream(rawName.split("_"))
                .skip(1)
                .collect(joining("_"));
    }

    public static String withoutScaleNumber(String rawName) {
        String[] components = rawName.split("_");
        return Arrays.stream(components)
                .limit(components.length - 1)
                .collect(joining("_"));
    }

    public static Builder builder() {
    	return new Builder();
    }

    public static class Builder {
    	private String rawName;
    	private String semanticName;

    	public Builder rawName(String rawName) {
    		this.rawName = rawName;
    		return this;
    	}

    	public Builder semanticName(String semanticName) {
    		this.semanticName = semanticName;
    		return this;
    	}

    	public ContainerName build() {
    		return new ContainerName(rawName, semanticName);
    	}

    }

}
