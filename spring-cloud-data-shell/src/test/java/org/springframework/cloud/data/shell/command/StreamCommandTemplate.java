/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.cloud.data.shell.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.data.shell.display.Table;
import org.springframework.cloud.data.shell.display.TableRow;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;

/**
 * Helper methods for stream commands to execute in the shell.
 * <p/>
 * It should mimic the client side API of StreamOperations as much as possible.
 *
 * @author Mark Pollack
 * @author Mark Fisher
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
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
	 *
	 * Note the name of the stream will be stored so that when the method destroyCreatedStreams is called, the stream
	 * will be destroyed.
	 *
	 * @param streamname the name of the stream
	 * @param streamdefinition the stream definition DSL
	 * @param values will be injected into streamdefinition according to {@link String#format(String, Object...)} syntax
	 */
	public void create(String streamname, String streamdefinition, Object... values) {
		doCreate(streamname, streamdefinition, true, values);
	}

	/**
	 * Execute stream create (but don't deploy) for the supplied stream name/definition, and verify the command result.
	 *
	 * Note the name of the stream will be stored so that when the method destroyCreatedStreams is called, the stream
	 * will be destroyed.
	 *
	 * @param values will be injected into streamdefinition according to {@link String#format(String, Object...)} syntax
	 */
	public void createDontDeploy(String streamname, String streamdefinition, Object... values) {
		doCreate(streamname, streamdefinition, false, values);
	}

	private void doCreate(String streamname, String streamdefinition, boolean deploy, Object... values) {
		String actualDefinition = String.format(streamdefinition, values);
		// Shell parser expects quotes to be escaped by \
		String wholeCommand = String.format("stream create %s --definition \"%s\" --deploy %s", streamname,
				actualDefinition.replaceAll("\"", "\\\\\""), deploy);
		CommandResult cr = shell.executeCommand(wholeCommand);
		//todo: Add deployment and verifier
//		if (deploy) {
//			stateVerifier.waitForDeploy(streamname);
//		}
//		else {
//			stateVerifier.waitForCreate(streamname);
//		}
		// add the stream name to the streams list before assertion
		streams.add(streamname);
		String deployMsg = "Created";
		if (deploy) {
			deployMsg = "Created and deployed";
		}
		assertEquals(deployMsg + " new stream '" + streamname + "'", cr.getResult());

		verifyExists(streamname, actualDefinition, deploy);
	}

	/**
	 * Deploy the given stream
	 *
	 * @param streamname name of the stream
	 */
	public void deploy(String streamname) {
		CommandResult cr = shell.executeCommand("stream deploy --name " + streamname);
		//stateVerifier.waitForDeploy(streamname);
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Deployed stream '" + streamname + "'", cr.getResult());
	}

	/**
	 * Destroy all streams that were created using the 'create' method. Commonly called in a @After annotated method
	 */
	public void destroyCreatedStreams() {
		for (int s = streams.size() - 1; s >= 0; s--) {
			String streamname = streams.get(s);
			CommandResult cr = shell.executeCommand("stream destroy --name " + streamname);
			//stateVerifier.waitForDestroy(streamname);
			assertTrue("Failure to destroy stream " + streamname + ".  CommandResult = " + cr.toString(),
					cr.isSuccess());
		}
	}

	/**
	 * Destroy a specific stream
	 *
	 * @param stream The stream to destroy
	 */
	public void destroyStream(String stream) {
		CommandResult cr = shell.executeCommand("stream destroy --name " + stream);
		//stateVerifier.waitForDestroy(stream);
		assertTrue("Failure to destroy stream " + stream + ".  CommandResult = " + cr.toString(),
				cr.isSuccess());
		streams.remove(stream);
	}

	/**
	 * Undeploy the given stream name
	 *
	 * @param streamname name of the stream.
	 */
	public void undeploy(String streamname) {
		CommandResult cr = shell.executeCommand("stream undeploy --name " + streamname);
		//stateVerifier.waitForUndeploy(streamname);
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
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		Table t = (Table) cr.getResult();
		assertTrue(t.getRows().contains(
				new TableRow().addValue(1, streamName).addValue(2, definition.replace("\\\\", "\\")).addValue(
						3,
						deployed ? "deployed" : "undeployed")));
	}

}
