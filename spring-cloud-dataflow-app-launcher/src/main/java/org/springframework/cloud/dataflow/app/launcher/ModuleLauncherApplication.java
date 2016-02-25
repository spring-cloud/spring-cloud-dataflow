/*
 * Copyright 2015-2016 the original author or authors.
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
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap for launching one or more modules, provided via the "modules" system property
 * or "MODULES" environment variable as a comma-delimited list, with the arguments
 * provided at launch.
 *
 * @see ModuleLauncherProperties for module and argument structure and
 * format
 *
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
@SpringBootApplication
public class ModuleLauncherApplication {

	public static void main(String[] args) throws Exception {
		List<String> argsList = new ArrayList<>(Arrays.asList(args));
		if (!argsList.contains("spring.jmx.default-domain")) {
			argsList.add("--spring.jmx.default-domain=module-launcher");
		}
		SpringApplication.run(ModuleLauncherApplication.class, argsList.toArray(new String[0]));
	}
}
