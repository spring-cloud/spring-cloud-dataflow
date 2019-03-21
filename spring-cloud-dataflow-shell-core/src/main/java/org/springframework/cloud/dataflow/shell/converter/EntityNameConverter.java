/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.AggregateCounterOperations;
import org.springframework.cloud.dataflow.rest.client.CounterOperations;
import org.springframework.cloud.dataflow.rest.client.FieldValueCounterOperations;
import org.springframework.cloud.dataflow.rest.client.JobOperations;
import org.springframework.cloud.dataflow.rest.client.StreamOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;

/**
 * A completer that provides completion for Data Flow entity names.
 *
 * @author Eric Bottard
 */
@Component
public class EntityNameConverter implements Converter<String> {

	@Autowired
	private DataFlowShell dataFlowShell;


	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return determineKind(optionContext).isPresent();
	}

	@Override
	public String convertFromText(String value, Class<?> targetType, String optionContext) {
		return value;
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
		try {
			String kind = determineKind(optionContext).get();
			switch (kind) {
				case "stream":
					streamOperations().list().forEach(sdr -> completions.add(new Completion(sdr.getName())));
					break;
				case "task":
					taskOperations().list().forEach(tdf -> completions.add(new Completion(tdf.getName())));
					break;
				case "counter":
					counterOperations().list().forEach(mr -> completions.add(new Completion(mr.getName())));
					break;
				case "field-value-counter":
					fieldValueCounterOperations().list().forEach(mr -> completions.add(new Completion(mr.getName())));
					break;
				case "aggregate-counter":
					aggregateCounterOperations().list().forEach(mr -> completions.add(new Completion(mr.getName())));
					break;
				case "job":
					jobOperations().executionList().forEach(jer -> {if (!"?".equals(jer.getName())) {
						completions.add(new Completion(jer.getName()));
					}});
					break;
				default:
					throw new AssertionError("Unsupported entity kind: " + kind);
			}
		} // Protect from exceptions in non-command code, as it would crash the whole shell
		catch (Exception e) {
		}
		return false;
	}

	private JobOperations jobOperations() {
		return dataFlowShell.getDataFlowOperations().jobOperations();
	}

	private AggregateCounterOperations aggregateCounterOperations() {
		return dataFlowShell.getDataFlowOperations().aggregateCounterOperations();
	}

	private FieldValueCounterOperations fieldValueCounterOperations() {
		return dataFlowShell.getDataFlowOperations().fieldValueCounterOperations();
	}

	private CounterOperations counterOperations() {
		return dataFlowShell.getDataFlowOperations().counterOperations();
	}

	private TaskOperations taskOperations() {
		return dataFlowShell.getDataFlowOperations().taskOperations();
	}

	private StreamOperations streamOperations() {
		return dataFlowShell.getDataFlowOperations().streamOperations();
	}

	private Optional<String> determineKind(String optionContext) {
		return Arrays.stream(optionContext.split("\\s+"))
			.filter(s -> s.startsWith("existing"))
			.map(s -> s.substring("existing-".length()))
			.findFirst();
	}
}
