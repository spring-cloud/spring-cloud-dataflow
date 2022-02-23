/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.cloud.dataflow.shell.config;

import java.util.List;

import org.jline.terminal.Terminal;

import org.springframework.shell.result.TerminalAwareResultHandler;
import org.springframework.shell.table.Table;

/**
 * Result handler which output's multiple tables.
 *
 * @author Janne Valkealahti
 */
public class TablesResultHandler extends TerminalAwareResultHandler<List<Table>> {

	public TablesResultHandler(Terminal terminal) {
		super(terminal);
	}

	@Override
	protected void doHandleResult(List<Table> tables) {
		for (Table table : tables) {
			terminal.writer().println(table.render(terminal.getWidth()));
		}
	}
}
