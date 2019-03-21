/*
 * Copyright 2015 the original author or authors.
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

import org.springframework.shell.table.BorderSpecification;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.SimpleHorizontalAligner;
import org.springframework.shell.table.SimpleVerticalAligner;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.Tables;

/**
 * Utility class to customize DataFlowTables used in a consistent way.
 *
 * @author Eric Bottard
 */
public class DataFlowTables {

	/**
	 * Customize the given TableBuilder with the following common features
	 * (these choices can always be overridden by applying later customizations) :<ul>
	 *     <li>double border around the whole table and first row</li>
	 *     <li>vertical space (air) borders, single line separators between rows</li>
	 *     <li>first row is assumed to be a header and is centered horizontally and vertically</li>
	 *     <li>cells containing Map values are rendered as {@literal key = value} lines, trying to align on equal signs</li>
	 * </ul>
	 */
	public static TableBuilder applyStyle(TableBuilder builder) {
		builder.addOutlineBorder(BorderStyle.fancy_double)
				.paintBorder(BorderStyle.air, BorderSpecification.INNER_VERTICAL)
				.fromTopLeft().toBottomRight()
				.paintBorder(BorderStyle.fancy_light, BorderSpecification.INNER_VERTICAL)
				.fromTopLeft().toBottomRight()
				.addHeaderBorder(BorderStyle.fancy_double)
				.on(CellMatchers.row(0))
				.addAligner(SimpleVerticalAligner.middle)
				.addAligner(SimpleHorizontalAligner.center)
		;
		return Tables.configureKeyValueRendering(builder, " = ");
	}

}
