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

package org.springframework.cloud.dataflow.app.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Spring boot {@link ApplicationRunner} that triggers {@link ModuleLauncher} to launch the modules.
 *
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 */
@Component
@EnableConfigurationProperties(ModuleLauncherProperties.class)
public class ModuleLauncherRunner implements CommandLineRunner {

	private final static Log log = LogFactory.getLog(ModuleLauncherRunner.class);

	private final static String GLOBAL_ARGS_KEY = "*";

	private final static String AGGREGATE_ARGS_KEY = "aggregate";

	@Autowired
	private ModuleLauncherProperties moduleLauncherProperties;

	@Autowired
	private ModuleLauncher moduleLauncher;

	@Override
	public void run(String... args) throws Exception {
		List<ModuleLaunchRequest> launchRequests = generateModuleLaunchRequests();
		if (log.isInfoEnabled()) {
			StringBuilder sb = new StringBuilder("Launching\n");
			for (ModuleLaunchRequest moduleLaunchRequest : launchRequests) {
				sb.append('\t').append(moduleLaunchRequest).append('\n');
			}
			log.info(sb.toString());
		}
		this.moduleLauncher.launch(launchRequests,
				moduleLauncherProperties.isAggregate(),
				moduleLauncherProperties.isAggregate() ?
						moduleLauncherProperties.getArgs().get(AGGREGATE_ARGS_KEY) :
						Collections.<String, String>emptyMap());
	}

	private List<ModuleLaunchRequest> generateModuleLaunchRequests() {
		List<ModuleLaunchRequest> requests = new ArrayList<>();
		String[] modules = this.moduleLauncherProperties.getModules();
		Map<String, Map<String, String>> arguments = this.moduleLauncherProperties.getArgs();
		for (int i = 0; i < modules.length; i++) {
			// merge the global arguments with the module specific arguments
			Map<String, String> moduleArguments = new HashMap<>();
			if (arguments != null) {
				// by inserting the global args first and the module specific args next
				// we ensure that the latter have precedence
				Map<String, String> globalArgs = arguments.get(GLOBAL_ARGS_KEY);
				if (globalArgs != null) {
					moduleArguments.putAll(globalArgs);
				}
				Map<String, String> moduleSpecificArgs = arguments.get(Integer.toString(i));
				if (moduleSpecificArgs != null) {
					moduleArguments.putAll(moduleSpecificArgs);
				}
			}
			ModuleLaunchRequest moduleLaunchRequest = new ModuleLaunchRequest(modules[i], moduleArguments);
			requests.add(moduleLaunchRequest);
		}
		return requests;
	}
}
