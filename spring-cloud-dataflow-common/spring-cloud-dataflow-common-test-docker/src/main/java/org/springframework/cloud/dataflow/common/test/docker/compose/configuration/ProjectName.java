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
package org.springframework.cloud.dataflow.common.test.docker.compose.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

@PackageVisible
public class ProjectName {

	private String projectName;

	public ProjectName(String projectName) {
		this.projectName = projectName;
		validate();
	}

	protected String projectName() {
		return projectName;
	}

	protected void validate() {
		Assert.state(projectName().trim().length() > 0, "ProjectName must not be blank.");
		Assert.state(validCharacters(),
				"ProjectName '" + projectName() + "' not allowed, please use lowercase letters and numbers only.");
	}

	// Only allows strings that docker-compose-cli would not modify
	// https://github.com/docker/compose/blob/85e2fb63b3309280a602f1f76d77d3a82e53b6c2/compose/cli/command.py#L84
	protected boolean validCharacters() {
		Predicate<String> illegalCharacters = Pattern.compile("[^a-z0-9]").asPredicate();
		return !illegalCharacters.test(projectName());
	}

	public String asString() {
		return projectName();
	}

	public List<String> constructComposeFileCommand() {
		return Arrays.asList("--project-name", projectName());
	}

	public static ProjectName random() {
		return ProjectName.of(UUID.randomUUID().toString().substring(0, 8));
	}

	/**
	 * A name consisting of lowercase letters and numbers only.
	 *
	 * @param name the name
	 * @return project name
	 */
	public static ProjectName fromString(String name) {
		return ProjectName.of(name);
	}

	private static ProjectName of(String name) {
		return new ProjectName(name);
	}
}
