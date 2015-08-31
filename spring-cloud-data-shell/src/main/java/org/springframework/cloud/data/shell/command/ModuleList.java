/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.shell.command;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cloud.data.core.ModuleType;
import org.springframework.cloud.data.rest.resource.ModuleRegistrationResource;
import org.springframework.shell.support.table.Table;
import org.springframework.shell.support.table.TableHeader;
import org.springframework.shell.support.table.TableRow;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Knows how to render a {@link Table} of {@link ModuleRegistrationResource}.
 *
 * @author Florent Biville
 * @author Eric Bottard
 */
class ModuleList {

	private Iterable<ModuleRegistrationResource> modules;

	private static final Map<String, Integer> typeToColumn = new LinkedHashMap<String, Integer>();

	static {
		for (int i = 0; i < ModuleType.values().length;) {
			typeToColumn.put(ModuleType.values()[i].name(), ++i); // 1 based
		}
	}

	public ModuleList(Iterable<ModuleRegistrationResource> modules) {
		Assert.notNull(modules);
		this.modules = modules;
	}

	public Table renderByType() {
		Table table = new Table();
		int i = 1;
		Map<String, Integer> currentRowByType = new HashMap<String, Integer>();
		for (String type : typeToColumn.keySet()) {
			table.addHeader(i++, new TableHeader("    " + StringUtils.capitalize(type)));
		}
		for (ModuleRegistrationResource module : modules) {
			TableRow row = rowForType(module.getType(), table, currentRowByType);
			row.addValue(typeToColumn.get(module.getType()), module.getName());
		}
		return table;
	}

	/**
	 * Return the row to which a module of the given type should be added.
	 * Will automatically grow the table if needed.
	 */
	private TableRow rowForType(String type, Table table, Map<String, Integer> currentRowByType) {
		Integer value = currentRowByType.get(type);
		if (value == null) {
			value = 0;
		}
		currentRowByType.put(type, value + 1);
		TableRow result;
		if (value >= table.getRows().size()) {
			result = new TableRow();
			for (int i = 1; i <= typeToColumn.size(); i++) {
				result.addValue(i, "");
			}
			table.getRows().add(result);
		}
		else {
			result = table.getRows().get(value);
		}
		return result;
	}
}
