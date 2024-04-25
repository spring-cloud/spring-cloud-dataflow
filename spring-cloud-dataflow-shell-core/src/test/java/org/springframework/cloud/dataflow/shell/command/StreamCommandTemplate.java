/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.dataflow.rest.resource.DeploymentStateResource;
import org.springframework.cloud.dataflow.shell.ShellCommandRunner;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Helper methods for stream commands to execute in the shell.
 * <p/>
 * It should mimic the client side API of StreamOperations as much as possible.
 *
 * @author Mark Pollack
 * @author Mark Fisher
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Chris Bono
 */
public class StreamCommandTemplate {

	private final ShellCommandRunner commandRunner;

	private final List<String> streams = new ArrayList<String>();

	/**
	 * Construct a new StreamCommandTemplate, given a spring shell.
	 *
	 * @param commandRunner the spring shell to execute commands against
	 */
	public StreamCommandTemplate(ShellCommandRunner commandRunner) {
		this.commandRunner = commandRunner;
	}

	/**
	 * Create and deploy a stream.
	 * <p>
	 * Note the name of the stream will be stored so that when the method
	 * destroyCreatedStreams is called, the stream will be destroyed.
	 *
	 * @param streamname the name of the stream
	 * @param streamdefinition the stream definition DSL
	 * @param values will be injected into streamdefinition according to
	 * {@link String#format(String, Object...)} syntax
	 */
	public void create(String streamname, String streamdefinition, Object... values) {
		doCreate(streamname, streamdefinition, true, values);
	}

	/**
	 * Execute stream create (but don't deploy) for the supplied stream name/definition,
	 * and verify the command result.
	 * <p>
	 * Note the name of the stream will be stored so that when the method
	 * destroyCreatedStreams is called, the stream will be destroyed.
	 *
	 * @param values will be injected into streamdefinition according to
	 * {@link String#format(String, Object...)} syntax
	 */
	public void createDontDeploy(String streamname, String streamdefinition, Object... values) {
		doCreate(streamname, streamdefinition, false, values);
	}

	private void doCreate(String streamname, String streamdefinition, boolean deploy, Object... values) {
		String actualDefinition = String.format(streamdefinition, values);
		// Shell parser expects quotes to be escaped by \
		String wholeCommand = String.format("stream create --name \"%s\" --definition \"%s\"", streamname,
				actualDefinition.replaceAll("\"", "\\\\\""));
		if (deploy) {
			wholeCommand += " --deploy";
		}
		Object result = commandRunner.executeCommand(wholeCommand);
		// todo: Add deployment and verifier
		// if (deploy) {
		// stateVerifier.waitForDeploy(streamname);
		// }
		// else {
		// stateVerifier.waitForCreate(streamname);
		// }
		// add the stream name to the streams list before assertion
		streams.add(streamname);
		String deployMsg = "Created new stream '" + streamname + "'";
		if (deploy) {
			deployMsg += "\nDeployment request has been sent";
		}
		assertThat(result).isEqualTo(deployMsg);

		verifyExists(streamname, actualDefinition, deploy);
	}

	/**
	 * Update the given stream
	 *
	 * @param streamname name of the stream
	 * @param propertyValue the value to update stream
	 *
	 */
	public void update(String streamname, String propertyValue, String expectedResult) {
		Object result = commandRunner.executeCommand("stream update --name " + streamname + " --properties " + propertyValue);
		assertThat((String)result).contains(expectedResult);
	}

	/**
	 * Update the given stream
	 *
	 * @param streamname name of the stream
	 * @param propertyFile the file that contains the properties
	 *
	 */
	public void updateFile(String streamname, String propertyFile, String expectedResult) {
		Object result = commandRunner.executeCommand("stream update --name " + streamname + " --propertiesFile " + propertyFile);
		assertThat((String)result).contains(expectedResult);
	}

	/**
	 * Deploy the given stream
	 *
	 * @param streamname name of the stream
	 */
	public void deploy(String streamname) {
		Object result = commandRunner.executeCommand("stream deploy --name " + streamname);
		// stateVerifier.waitForDeploy(streamname);
		assertThat(result).isEqualTo("Deployed stream '" + streamname + "'");
	}

	/**
	 * Validate the given stream
	 *
	 * @param streamName name of the stream
	 */
	public Object validate(String streamName) {
		return commandRunner.executeCommand("stream validate --name " + streamName);
	}

	/**
	 * Destroy all streams that were created using the 'create' method. Commonly called in
	 * a @After annotated method
	 */
	public void destroyCreatedStreams() {
		for (int s = streams.size() - 1; s >= 0; s--) {
			String streamname = streams.get(s);
			commandRunner.executeCommand("stream destroy --name " + streamname);
			// stateVerifier.waitForDestroy(streamname);
		}
	}

	/**
	 * Destroy a specific stream
	 *
	 * @param stream The stream to destroy
	 */
	public void destroyStream(String stream) {
		commandRunner.executeCommand("stream destroy --name " + stream);
		// stateVerifier.waitForDestroy(stream);
		streams.remove(stream);
	}

	/**
	 * Undeploy the given stream name
	 *
	 * @param streamname name of the stream.
	 */
	public void undeploy(String streamname) {
		commandRunner.executeCommand("stream undeploy --name " + streamname);
		// stateVerifier.waitForUndeploy(streamname);
	}

	/**
	 * Verify the stream is listed in stream list.
	 *
	 * @param streamName the name of the stream
	 * @param definition definition of the stream
	 */
	public void verifyExists(String streamName, String definition, boolean deployed) {
		Object result = commandRunner.executeCommand("stream list");
		assertThat(result).isInstanceOf(Table.class);
		Table table = (Table) result;
		TableModel model = table.getModel();
		Collection<String> statuses = deployed
				? Arrays.asList(DeploymentStateResource.DEPLOYED.getDescription(),
				DeploymentStateResource.DEPLOYING.getDescription())
				: Collections.singletonList(DeploymentStateResource.UNDEPLOYED.getDescription());
		for (int row = 0; row < model.getRowCount(); row++) {
			if (streamName.equals(model.getValue(row, 0))
					&& definition.replace("\\\\", "\\").equals(model.getValue(row, 2))) {
				// TODO (Tzolov) CLASSIC-MODE-REMOVAL To compute an aggregated state the Info returned by the mocked
				// TODO SkipperClient.info() (in SkipperStreamDeployer#getStreamDeploymentState) must have a
				// TODO valid PlatformStatus
				// && statuses.contains(model.getValue(row, 2))) {
				return;
			}
		}
		fail("Stream named " + streamName + " does not exist");

	}

}
