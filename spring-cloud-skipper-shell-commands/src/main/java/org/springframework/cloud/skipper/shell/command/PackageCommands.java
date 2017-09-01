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
public class PackageCommands {

	private SkipperClient skipperClient;

	@Autowired
	public PackageCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	@ShellMethod(key = "package list", value = "Get the package metadata")
	public String packageMetadata(
			@ShellOption(help = "boolean to set for more detailed package metadata", defaultValue = "false") boolean details) {
		return skipperClient.getPackageMetadata(details);
	}

	@EventListener
	void handle(SkipperClientUpdatedEvent event) {
		this.skipperClient = event.getSkipperClient();
	}
}
