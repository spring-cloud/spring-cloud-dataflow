/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.List;

import org.springframework.cloud.dataflow.rest.resource.DeploymentStateResource;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 */
public class StreamCommandTemplate {

	private final JLineShellComponent shell;

	private List<String> streams = new ArrayList<String>();

	/**
	 * Construct a new StreamCommandTemplate, given a spring shell.
	 *
	 * @param shell the spring shell to execute commands against
	 */
	public StreamCommandTemplate(JLineShellComponent shell) {
		this.shell = shell;
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
		String wholeCommand = String.format("stream create --name \"%s\" --definition \"%s\" --deploy %s", streamname,
				actualDefinition.replaceAll("\"", "\\\\\""), deploy);
		CommandResult cr = shell.executeCommand(wholeCommand);
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
		assertEquals(deployMsg, cr.getResult());

		verifyExists(streamname, actualDefinition, deploy);
	}

	/**
	 * Deploy the given stream
	 *
	 * @param streamname name of the stream
	 */
	public void deploy(String streamname) {
		CommandResult cr = shell.executeCommand("stream deploy --name " + streamname);
		// stateVerifier.waitForDeploy(streamname);
		assertTrue(cr.isSuccess(), "Failure.  CommandResult = " + cr.toString());
		assertEquals("Deployed stream '" + streamname + "'", cr.getResult());
	}

	/**
	 * Validate the given stream
	 *
	 * @param streamName name of the stream
	 */
	public CommandResult validate(String streamName) {
		CommandResult cr = shell.executeCommand("stream validate --name " + streamName);
		assertTrue(cr.isSuccess(), "Failure.  CommandResult = " + cr.toString());
		return cr;
	}

	/**
	 * Destroy all streams that were created using the 'create' method. Commonly called in
	 * a @After annotated method
	 */
	public void destroyCreatedStreams() {
		for (int s = streams.size() - 1; s >= 0; s--) {
			String streamname = streams.get(s);
			CommandResult cr = shell.executeCommand("stream destroy --name " + streamname);
			// stateVerifier.waitForDestroy(streamname);
			assertTrue(cr.isSuccess(),
					"Failure to destroy stream " + streamname + ".  CommandResult = " + cr.toString());
		}
	}

	/**
	 * Destroy a specific stream
	 *
	 * @param stream The stream to destroy
	 */
	public void destroyStream(String stream) {
		CommandResult cr = shell.executeCommand("stream destroy --name " + stream);
		// stateVerifier.waitForDestroy(stream);
		assertTrue(cr.isSuccess(), "Failure to destroy stream " + stream + ".  CommandResult = " + cr.toString());
		streams.remove(stream);
	}

	/**
	 * Undeploy the given stream name
	 *
	 * @param streamname name of the stream.
	 */
	public void undeploy(String streamname) {
		CommandResult cr = shell.executeCommand("stream undeploy --name " + streamname);
		// stateVerifier.waitForUndeploy(streamname);
		assertTrue(cr.isSuccess());
		assertEquals("Un-deployed stream '" + streamname + "'", cr.getResult());
	}

	/**
	 * Verify the stream is listed in stream list.
	 *
	 * @param streamName the name of the stream
	 * @param definition definition of the stream
	 */
	public void verifyExists(String streamName, String definition, boolean deployed) {
		CommandResult cr = shell.executeCommand("stream list");
		assertTrue(cr.isSuccess(), "Failure.  CommandResult = " + cr.toString());

		Table table = (org.springframework.shell.table.Table) cr.getResult();
		TableModel model = table.getModel();
		Collection<String> statuses = deployed
				? Arrays.asList(DeploymentStateResource.DEPLOYED.getDescription(),
				DeploymentStateResource.DEPLOYING.getDescription())
				: Arrays.asList(DeploymentStateResource.UNDEPLOYED.getDescription());
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
