/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.module.deployer.lattice;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.data.module.ModuleStatus;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * @author Patrick Peralta
 */
public class ReceptorModuleDeployer implements ModuleDeployer {
	private static final Logger logger = LoggerFactory.getLogger(ReceptorModuleDeployer.class);


	@Override
	public void deploy(ModuleDescriptor descriptor) {
		throw new UnsupportedOperationException("todo");
	}

	@Override
	public void undeploy(ModuleDescriptor.Key key) {
		throw new UnsupportedOperationException("todo");
	}

	@Override
	public ModuleStatus status(ModuleDescriptor.Key descriptor) {
		throw new UnsupportedOperationException("todo");
	}

	@Override
	public Map<ModuleDescriptor.Key, ModuleStatus> status() {
		throw new UnsupportedOperationException("todo");
	}
}