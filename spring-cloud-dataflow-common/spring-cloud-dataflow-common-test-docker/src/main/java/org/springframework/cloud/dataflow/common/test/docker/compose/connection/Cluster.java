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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class Cluster {

	private final String ip;
	private final ContainerCache containerCache;

    public Cluster(String ip, ContainerCache containerCache) {
		this.ip = ip;
		this.containerCache = containerCache;
	}

    public String ip() {
    	return ip;
    }

    public ContainerCache containerCache() {
    	return containerCache;
    }

    public Container container(String name) {
        return containerCache().container(name);
    }

    public List<Container> containers(List<String> containerNames) {
        return containerNames.stream()
                .map(this::container)
                .collect(toList());
    }

    public Set<Container> allContainers() throws IOException, InterruptedException {
        return containerCache().containers();
    }

	public static Builder builder() {
    	return new Builder();
    }

    public static class Builder {

    	private String ip;
    	private ContainerCache containerCache;

    	public Builder ip(String ip) {
    		this.ip = ip;
    		return this;
    	}

    	public Builder containerCache(ContainerCache containerCache) {
    		this.containerCache = containerCache;
    		return this;
    	}

    	public Cluster build() {
    		return new Cluster(ip, containerCache);
    	}
    }
}
