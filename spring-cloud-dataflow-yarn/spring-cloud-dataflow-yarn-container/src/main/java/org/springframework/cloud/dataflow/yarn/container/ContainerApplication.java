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

package org.springframework.cloud.dataflow.yarn.container;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.module.launcher.ModuleLaunchRequest;
import org.springframework.cloud.stream.module.launcher.ModuleLauncher;
import org.springframework.cloud.stream.module.launcher.ModuleLauncherConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.yarn.annotation.OnContainerStart;
import org.springframework.yarn.annotation.YarnComponent;
import org.springframework.yarn.annotation.YarnParameter;
import org.springframework.yarn.annotation.YarnParameters;
import org.springframework.yarn.container.YarnContainerSupport;

/**
 * Yarn application bootstrapping container and running modules.
 *
 * @author Janne Valkealahti
 *
 */
@SpringBootApplication
@Import(ModuleLauncherConfiguration.class)
@YarnComponent
public class ContainerApplication extends YarnContainerSupport {

	private final static Log log = LogFactory.getLog(ContainerApplication.class);

	@Autowired
	private ModuleLauncher moduleLauncher;

	@OnContainerStart
	public Future<Boolean> runModule(@YarnParameters Properties properties, @YarnParameter("containerModules") String module) {
		
		if (log.isInfoEnabled()) {
			log.info("runModule module=" + module);
			log.info("runModule properties=" + properties);
			log.info("moduleLauncher=" + moduleLauncher);
		}
		
		Map<String, String> args = new HashMap<>();

		for (Entry<Object, Object> entry : properties.entrySet()) {
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			if (!key.startsWith("containerModules")) {
				args.put(key, value);
			}
		}
		
		if (log.isInfoEnabled()) {
			log.info("Passing args to moduleLauncher: " + args);
		}
		
		// we should somehow get status back from module
		// launcher when it fails or finishes to set future
		// indicating we're done. Naturally exception will
		// terminate execution chain and container will exit.
		SettableListenableFuture<Boolean> status = new SettableListenableFuture<>();
		moduleLauncher.launch(Arrays.asList(new ModuleLaunchRequest(module, args)));
		return status;
	}

	public static void main(String[] args) {
		SpringApplication.run(ContainerApplication.class, args);
	}

}
