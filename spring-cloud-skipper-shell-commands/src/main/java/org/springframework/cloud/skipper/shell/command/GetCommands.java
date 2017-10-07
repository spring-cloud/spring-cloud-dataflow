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

import java.util.Map;

import javax.validation.constraints.NotNull;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.http.HttpStatus;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.HttpStatusCodeException;

import static org.springframework.shell.standard.ShellOption.NULL;

/**
 * @author Mark Pollack
 */
@ShellComponent
public class GetCommands extends AbstractSkipperCommand {

	private Yaml yaml;

	@Autowired
	public GetCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		this.yaml = new Yaml(dumperOptions);
	}

	@ShellMethod(key = "get manifest", value = "Status for a last known release version.")
	public Object getManifest(
			@ShellOption(help = "release name") @NotNull String releaseName,
			@ShellOption(help = "the specific release version.", defaultValue = NULL) Integer releaseVersion) {
		String manifest;
		try {
			if (releaseVersion == null) {
				manifest = this.skipperClient.manifest(releaseName);
			}
			else {
				manifest = this.skipperClient.manifest(releaseName, releaseVersion);
			}
		}
		catch (HttpStatusCodeException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				// 404 means release not found.
				// TODO it'd be nice to rethrow ReleaseNotFoundException in
				// SkipperClient but that exception is on server
				return "Release with name '" + releaseName + "' not found";
			}
			// if something else, rethrow
			throw e;
		}
		return manifest;
	}

	private String formatManifest(String manifest) {
		Map<String, String> manifestAsMap = (Map<String, String>) yaml.load(manifest);
		return yaml.dump(manifestAsMap);
	}
}
