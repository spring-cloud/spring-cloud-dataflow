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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

/**
 * Configuration properties for {@link ModuleLauncher}.
 *
 * <p>Expects the following keys (resolved from the {@link Environment}, so this could take many forms _ System
 * properties, environment variables, program arguments, <i>etc.</i> _):<ul>
 *     <li>{@literal modules = <list>}: an ordered list of maven coordinates of modules to launch</li>
 *     <li>{@literal args[<index>][<key>] = <value>}: key/value pairs that will become module arguments,
 *     where {@literal <index>} is the 0-based index of the module in the list above and '*' can be used for passing
 *     arguments to all modules</li>
 *     <li>{@literal aggregate = true | false}</li>: whether multiple modules launched together should be aggregated,
 *     case in which they will be launched as a single individual unit, and {@literal args['aggregate'][<key>] = <value>}
 *     can be used for passing arguments to the aggregate;
 * </ul>
 *
 * As an example, this is how one would launch the {@literal time --fixedDelay=4 | log} canonical example:
 * <pre>
 *     modules = org.springframework.cloud.modules:time-source:1.0.0-SNAPSHOT,org.springframework.cloud.modules:log-sink:1.0.0-SNAPSHOT
 *     args.0.fixedDelay=4
 * </pre>
 *
 * And this is how one would launch the {@literal time --fixedDelay=4 | log} example as an aggregate:
 * <pre>
 *     modules = org.springframework.cloud.modules:time-source:1.0.0-SNAPSHOT,org.springframework.cloud.modules:log-sink:1.0.0-SNAPSHOT
 *     args.0.fixedDelay=4
 *     aggregate=true
 * </pre>
 * </p>
 *
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Eric Bottard
 */
@ConfigurationProperties
public class ModuleLauncherProperties {

	/**
	 * True if aggregating multiple modules when launched together
	 */
	private boolean aggregate;

	/**
	 * File path to a locally available maven repository, where modules will be downloaded.
	 */
	private String[] modules;

	/**
	 * Map of arguments, keyed by the 0-based index in the {@link #modules array}, allowing for an additional generic
	 * key for global arguments.
	 */
	private Map<String, Map<String, String>> args = new HashMap<>();

	public boolean isAggregate() {
		return aggregate;
	}

	public void setAggregate(boolean aggregate) {
		this.aggregate = aggregate;
	}

	public void setModules(String[] modules) {
		this.modules = modules;
	}

	@NotEmpty(message = "A list of modules must be specified.")
	public String[] getModules() {
		return modules;
	}

	public void setArgs(Map<String, Map<String, String>> args) {
		this.args = args;
	}

	public Map<String, Map<String, String>> getArgs() {
		return args;
	}
}
