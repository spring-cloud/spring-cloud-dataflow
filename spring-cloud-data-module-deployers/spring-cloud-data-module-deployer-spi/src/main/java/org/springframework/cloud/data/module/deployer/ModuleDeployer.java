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

package org.springframework.cloud.data.module.deployer;


import java.util.Map;

import org.springframework.cloud.data.module.ModuleStatus;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Interface specifying the operations for a runtime environment
 * capable of launching {@link ModuleDescriptor modules}.
 *
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public interface ModuleDeployer {

	/**
	 * Deploy the given {@code ModuleDescriptor}. Implementations
	 * may perform this operation asynchronously; therefore
	 * a successful deployment may not be assumed upon return.
	 * To determine the status of a deployment, invoke
	 * {@link #status(org.springframework.xd.module.ModuleDescriptor.Key)}.
	 *
	 * @param descriptor descriptor for module to be deployed
	 *
	 * @throws IllegalStateException if the module has already been deployed
	 */
	void deploy(ModuleDescriptor descriptor);

	/**
	 * Un-deploy the the given {@code ModuleDescriptor}. Implementations
	 * may perform this operation asynchronously; therefore
	 * a successful un-deployment may not be assumed upon return.
	 * To determine the status of a deployment, invoke
	 * {@link #status(org.springframework.xd.module.ModuleDescriptor.Key)}.
	 *
	 * @param key unique key for module to be un-deployed
	 *
	 * @throws IllegalStateException if the module has not been deployed
	 */
	void undeploy(ModuleDescriptor.Key key);

	/**
	 * Return the deployment status of the given {@code ModuleDescriptor}.
	 *
	 * @param key of the {@link ModuleDescriptor} this status is for
	 *
	 * @return module deployment status
	 */
	ModuleStatus status(ModuleDescriptor.Key key);

	/**
	 * Return a map of all deployed {@code ModuleDescriptor}s.
	 *
	 * @return map of deployed {@code ModuleDescriptor}s.
	 */
	Map<ModuleDescriptor.Key, ModuleStatus> allStatuses();

}
