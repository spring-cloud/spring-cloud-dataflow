/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.shell.command.support.SkipperClientUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * @author Ilayaperumal Gopinathan
 */
@ShellComponent
public class ReleaseCommands {

	private SkipperClient skipperClient;

	@Autowired
	public ReleaseCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	@ShellMethod(key = "release undeploy", value = "Undeploy a release")
	public String undeploy(
			@ShellOption(help = "the name of the release to undeploy") String releaseName) {
		return skipperClient.undeploy(releaseName);
	}

	@ShellMethod(key = "release rollback", value = "Rollback to a previous release")
	public String rollback(
			@ShellOption(help = "the name of the release to rollback") String releaseName,
			@ShellOption(help = "the specific release version to rollback to. " +
					"Not specifying the value rolls back to the previous release.", defaultValue = "0") int releaseVersion) {
		return skipperClient.rollback(releaseName, releaseVersion);
	}

	@EventListener
	void handle(SkipperClientUpdatedEvent event) {
		this.skipperClient = event.getSkipperClient();
	}
}
