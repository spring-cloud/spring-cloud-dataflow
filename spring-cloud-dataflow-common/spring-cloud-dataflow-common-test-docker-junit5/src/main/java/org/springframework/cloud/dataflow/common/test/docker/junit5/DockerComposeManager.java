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
package org.springframework.cloud.dataflow.common.test.docker.junit5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.cloud.dataflow.common.test.docker.compose.DockerComposeRule;
import org.springframework.cloud.dataflow.common.test.docker.compose.DockerComposeRule.Builder;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DockerComposeFiles;

import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.HealthChecks.toHaveAllPortsOpen;

/**
 *
 * @author Janne Valkealahti
 *
 */
public class DockerComposeManager {

	private final Map<String, DockerComposeRule> rules = new HashMap<>();
	private final Map<String, List<DockerComposeData>> classKeys = new HashMap<>();
	private final Map<String, List<DockerComposeData>> methodKeys = new HashMap<>();

	public DockerComposeManager() {}

	public void addClassDockerComposeData(String classKey, DockerComposeData dockerComposeData) {
		String key = dockerComposeData.id + "$" + classKey;
		classKeys.putIfAbsent(key, new ArrayList<>());
		classKeys.get(key).add(dockerComposeData);
	}

	public void addMethodDockerComposeData(String classKey, String methodKey, DockerComposeData dockerComposeData) {
		String key = dockerComposeData.id + "$" + classKey;
		if (classKeys.containsKey(key)) {
			classKeys.get(key).add(dockerComposeData);
		}
		else {
			key = dockerComposeData.id + "$" + classKey + methodKey;
			methodKeys.putIfAbsent(key, new ArrayList<>());
			methodKeys.get(key).add(dockerComposeData);
		}
	}

	public DockerComposeRule getRule(String id) {
		for (Entry<String, DockerComposeRule> e : rules.entrySet()) {
			String idMatch = e.getKey().substring(0, e.getKey().indexOf("$"));
			if (id.equals(idMatch)) {
				return e.getValue();
			}
		}
		throw new IllegalArgumentException("Id " + id + " not found");
	}

	public void build(String classKey, String methodKey) {

		ArrayList<OrderingWrapper> toStart = new ArrayList<>();

		// class level
		for (Entry<String, List<DockerComposeData>> e : classKeys.entrySet()) {
			String key = e.getKey();
			ArrayList<String> locations = new ArrayList<>();
			ArrayList<String> services = new ArrayList<>();
			boolean start = true;
			Integer order = Integer.MAX_VALUE;
			String log = "";
			for (DockerComposeData dockerComposeData : e.getValue()) {
				locations.addAll(Arrays.asList(dockerComposeData.getLocations()));
				services.addAll(Arrays.asList(dockerComposeData.getServices()));
				if (!dockerComposeData.isStart()) {
					start = false;
				}
				if (dockerComposeData.getOrder() < order) {
					order = dockerComposeData.getOrder();
				}
				if (dockerComposeData.getLog() != null && dockerComposeData.getLog().length() > 0) {
					log = dockerComposeData.getLog();
				}
			}
			Builder<?> builder = DockerComposeRule.builder();
			builder.files(DockerComposeFiles.from(locations.toArray(new String[0])));
			for (String service : services) {
				builder.waitingForService(service, toHaveAllPortsOpen(), DockerComposeRule.DEFAULT_TIMEOUT);
			}
			builder.saveLogsTo("build/test-docker-logs/" + log + classKey + "-" + methodKey);
			DockerComposeRule rule = builder.build();
			rules.put(key, rule);
			if (start) {
				toStart.add(new OrderingWrapper(order, rule));
			}
		}

		// method level
		for (Entry<String, List<DockerComposeData>> e : methodKeys.entrySet()) {
			String key = e.getKey();
			ArrayList<String> locations = new ArrayList<>();
			ArrayList<String> services = new ArrayList<>();
			boolean start = true;
			Integer order = Integer.MAX_VALUE;
			String log = "";
			for (DockerComposeData dockerComposeData : e.getValue()) {
				locations.addAll(Arrays.asList(dockerComposeData.getLocations()));
				services.addAll(Arrays.asList(dockerComposeData.getServices()));
				if (!dockerComposeData.isStart()) {
					start = false;
				}
				if (dockerComposeData.getOrder() < order) {
					order = dockerComposeData.getOrder();
				}
				if (dockerComposeData.getLog() != null && dockerComposeData.getLog().length() > 0) {
					log = dockerComposeData.getLog();
				}
			}
			Builder<?> builder = DockerComposeRule.builder();
			builder.files(DockerComposeFiles.from(locations.toArray(new String[0])));
			for (String service : services) {
				builder.waitingForService(service, toHaveAllPortsOpen(), DockerComposeRule.DEFAULT_TIMEOUT);
			}
			builder.saveLogsTo("build/test-docker-logs/" + log + classKey + "-" + methodKey);
			DockerComposeRule rule = builder.build();
			rules.put(key, rule);
			if (start) {
				toStart.add(new OrderingWrapper(order, rule));
			}
		}

		Collections.sort(toStart);
		for (OrderingWrapper w : toStart) {
			try {
				w.getRule().before();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

	public void stop(String classKey, String methodKey) {
		ArrayList<String> toRemove = new ArrayList<>();
		for (Entry<String, DockerComposeRule> e : rules.entrySet()) {
			String idMatch = e.getKey().substring(e.getKey().indexOf("$") + 1, e.getKey().length());
			if (idMatch.equals(classKey)) {
				toRemove.add(e.getKey());
			}
			if (idMatch.equals(classKey + methodKey)) {
				toRemove.add(e.getKey());
			}
		}
		for (String remove : toRemove) {
			DockerComposeRule rule = rules.remove(remove);
			if (rule != null) {
				rule.after();
			}
		}
		// for now, just clear both class and method keys
		classKeys.clear();
		methodKeys.clear();
	}

	public void startId(String id) {
		DockerComposeRule rule = null;
		for (Entry<String, DockerComposeRule> e : rules.entrySet()) {
			String idMatch = e.getKey().substring(0, e.getKey().indexOf("$"));
			if (id.equals(idMatch)) {
				rule = e.getValue();
			}
		}
		if (rule != null) {
			try {
				rule.before();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void stopId(String id) {
		DockerComposeRule rule = null;
		for (Entry<String, DockerComposeRule> e : rules.entrySet()) {
			String idMatch = e.getKey().substring(0, e.getKey().indexOf("$"));
			if (id.equals(idMatch)) {
				rule = e.getValue();
			}
		}
		if (rule != null) {
			rule.after();
		}
	}

	public static class DockerComposeData {

		private final String id;
		private final boolean start;
		private final String[] locations;
		private final String[] services;
		private final String log;
		private final int order;

		public DockerComposeData(String id, String[] locations, String[] services, String log, boolean start, int order) {
			this.id = id;
			this.locations = locations;
			this.services = services;
			this.log = log;
			this.start = start;
			this.order = order;
		}

		public String[] getLocations() {
			return locations;
		}

		public String[] getServices() {
			return services;
		}

		public String getLog() {
			return log;
		}

		public String getId() {
			return id;
		}

		public boolean isStart() {
			return start;
		}

		public int getOrder() {
			return order;
		}
	}

	private static class OrderingWrapper implements Comparable<OrderingWrapper>{
		Integer order;
		DockerComposeRule rule;

		public OrderingWrapper(Integer order, DockerComposeRule rule) {
			this.order = order;
			this.rule = rule;
		}

		public Integer getOrder() {
			return order;
		}

		public DockerComposeRule getRule() {
			return rule;
		}

		@Override
		public int compareTo(OrderingWrapper o) {
			return getOrder().compareTo(o.getOrder());
		}
	}
}
