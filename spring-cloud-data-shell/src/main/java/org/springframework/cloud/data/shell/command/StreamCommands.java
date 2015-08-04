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

package org.springframework.cloud.data.shell.command;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.data.rest.client.StreamOperations;
import org.springframework.cloud.data.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.data.shell.config.RESTClientShell;
import org.springframework.cloud.data.shell.util.Table;
import org.springframework.cloud.data.shell.util.TableHeader;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Stream commands
 *
 * @author Ilayaperumal Gopinathan
 */

@Component
public class StreamCommands implements CommandMarker {

	private static final String CREATE_STREAM = "stream create";

	private static final String LIST_STREAM = "stream list";

	@Autowired
	private RESTClientShell RESTClientShell;

	@CliAvailabilityIndicator({ CREATE_STREAM, LIST_STREAM })
	public boolean available() {
		return RESTClientShell.getRESTClientOperations() != null;
	}

	@CliCommand(value = CREATE_STREAM, help = "Create a new stream definition")
	public String createStream(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the stream") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "a stream definition, using XD DSL (e.g. \"http --port=9000 | hdfs\")") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the stream immediately", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean deploy) {
		streamOperations().createStream(name, dsl, deploy);
		return (deploy) ? String.format("Created and deployed new stream '%s'", name) : String.format(
				"Created new stream '%s'", name);
	}

	@CliCommand(value = LIST_STREAM, help = "List created streams")
	public Table listStreams() {

		final PagedResources<StreamDefinitionResource> streams = streamOperations().list();

		final Table table = new Table()
				.addHeader(1, new TableHeader("Stream Name"))
				.addHeader(2, new TableHeader("Stream Definition"))
				.addHeader(3, new TableHeader("Status"));

		for (StreamDefinitionResource stream : streams) {
			table.newRow()
					.addValue(1, stream.getName())
					.addValue(2, stream.getDefinition())
					.addValue(3, stream.getStatus());
		}
		return table;
	}

	StreamOperations streamOperations() {
		return RESTClientShell.getRESTClientOperations().streamOperations();
	}
}
