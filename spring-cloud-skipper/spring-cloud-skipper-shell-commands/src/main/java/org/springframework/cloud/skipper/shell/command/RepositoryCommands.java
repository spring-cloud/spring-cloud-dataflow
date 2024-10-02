/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.shell.command.support.TableUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

/**
 * Commands to add, delete and list package repositories.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 */
@ShellComponent
public class RepositoryCommands extends AbstractSkipperCommand {

	public RepositoryCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	@ShellMethod(key = "repo list", value = "List package repositories")
	public Table list() {
		Collection<Repository> repositoryResources = this.skipperClient.listRepositories();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Name");
		headers.put("url", "URL");
		headers.put("local", "Local");
		headers.put("repoOrder", "Order");
		TableModel model = new BeanListTableModel<>(repositoryResources, headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		return TableUtils.applyStyle(tableBuilder).build();
	}

}
