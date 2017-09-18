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

import java.util.LinkedHashMap;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.shell.command.support.TableUtils;
import org.springframework.hateoas.Resources;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

import static org.springframework.shell.standard.ShellOption.NULL;

/**
 * Commands to add, delete and list package repositories
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 */
@ShellComponent
public class RepositoryCommands extends AbstractSkipperCommand {

	@Autowired
	public RepositoryCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	@ShellMethod(key = "repository add", value = "Add package repository")
	public String add(@ShellOption(help = "name of the repository") @NotNull String name,
			@ShellOption(help = "the root URL that points to index.yaml file") @NotNull String url,
			@ShellOption(help = "the source URL to the package", defaultValue = NULL) String sourceUrl) {
		Repository repository = this.skipperClient.addRepository(name, url, sourceUrl);
		return "Repository '" + name + "' added.";
	}

	@ShellMethod(key = "repository delete", value = "Delete a package repository")
	public String delete(@ShellOption(help = "name of the repository to delete") @NotNull String name) {
		this.skipperClient.deleteRepository(name);
		return "Repository '" + name + "' deleted.";
	}

	@ShellMethod(key = "repository list", value = "List package repositories")
	public Table list() {
		Resources<Repository> repositoryResources = this.skipperClient.listRepositories();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Name");
		headers.put("url", "Root URL");
		headers.put("sourceUrl", "Source URL");
		TableModel model = new BeanListTableModel<>(repositoryResources.getContent(), headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		return TableUtils.applyStyle(tableBuilder).build();
	}

}
