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

package org.springframework.cloud.data.rest;

import java.util.Iterator;

import org.springframework.cloud.data.core.ModuleCoordinates;
import org.springframework.cloud.data.core.ModuleDefinition;
import org.springframework.cloud.data.core.ModuleDeploymentRequest;
import org.springframework.cloud.data.core.StreamDefinition;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.cloud.data.module.deployer.local.LocalModuleDeployer;
import org.springframework.cloud.data.module.registry.ModuleRegistry;
import org.springframework.cloud.data.module.registry.StubModuleRegistry;
import org.springframework.cloud.data.repository.StreamDefinitionRepository;
import org.springframework.cloud.data.repository.StubStreamDefinitionRepository;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Mark Fisher
 */
@RestController
@RequestMapping("/streams")
public class StreamController {

	private final StreamDefinitionRepository repository = new StubStreamDefinitionRepository();

	private final ModuleRegistry registry = new StubModuleRegistry();

	private final ModuleDeployer deployer = new LocalModuleDeployer();

	@RequestMapping
	public String list() {
		StringBuilder builder = new StringBuilder();
		for (StreamDefinition definition : this.repository.findAll()) {
			builder.append(String.format("%s\n", definition));
		}
		return builder.toString();
	}

	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	public void create(@PathVariable("name") String name, @RequestBody String dsl) {
		this.repository.save(new StreamDefinition(name, dsl));
	}

	@RequestMapping(value = "/{name}", method = RequestMethod.PUT)
	public void deploy(@PathVariable("name") String name) {
		StreamDefinition streamDefinition = this.repository.findByName(name);
		Iterator<ModuleDefinition> iterator = streamDefinition.getDeploymentOrderIterator();
		for (int i = 0; iterator.hasNext(); i++) {
			String type = (i == 0) ? "sink" : "source"; // TODO: support processors
			ModuleDefinition definition = iterator.next();
			ModuleCoordinates coordinates = this.registry.findByNameAndType(definition.getName(), type);
			Assert.notNull(coordinates, String.format("unable to find coordinates for %s", definition));
			this.deployer.deploy(new ModuleDeploymentRequest(definition, coordinates));
		}
	}

}
