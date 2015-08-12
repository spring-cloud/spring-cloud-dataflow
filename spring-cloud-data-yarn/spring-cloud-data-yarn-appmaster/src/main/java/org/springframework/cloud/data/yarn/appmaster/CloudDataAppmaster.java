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

package org.springframework.cloud.data.yarn.appmaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.Container;
import org.springframework.yarn.am.cluster.ContainerCluster;
import org.springframework.yarn.am.cluster.ManagedContainerClusterAppmaster;

/**
 * Custom yarn appmaster tweaking container launch settings.
 *
 * @author Janne Valkealahti
 *
 */
public class CloudDataAppmaster extends ManagedContainerClusterAppmaster {

	private final static Log log = LogFactory.getLog(CloudDataAppmaster.class);

	@Override
	protected List<String> onContainerLaunchCommands(Container container, ContainerCluster cluster,
			List<String> commands) {

		ArrayList<String> list = new ArrayList<String>(commands);
		Map<String, Object> extraProperties = cluster.getExtraProperties();

		log.debug("onContainerLaunchCommands extraProperties=" + extraProperties);

		if (extraProperties != null && extraProperties.containsKey("containerModules")) {
			String value = "containerModules=" + cluster.getExtraProperties().get("containerModules");
			list.add(Math.max(list.size() - 2, 0), value);
		}
		list.add(1, "-Dserver.port=0");
		return list;
	}

}
