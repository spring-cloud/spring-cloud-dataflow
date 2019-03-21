/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command.common;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.shell.table.BorderSpecification;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.Formatter;
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
	 * Customize the given TableBuilder with the following common features (these choices
	 * can always be overridden by applying later customizations) :
	 * <ul>
	 * <li>double border around the whole table and first row</li>
	 * <li>vertical space (air) borders, single line separators between rows</li>
	 * <li>first row is assumed to be a header and is centered horizontally and
	 * vertically</li>
	 * <li>cells containing Map values are rendered as {@literal key = value} lines,
	 * trying to align on equal signs</li>
	 * </ul>
	 *
	 * @param builder the table builder to use
	 * @return the configured table builder
	 */
	public static TableBuilder applyStyle(TableBuilder builder) {
		builder.addOutlineBorder(BorderStyle.fancy_double)
				.paintBorder(BorderStyle.air, BorderSpecification.INNER_VERTICAL).fromTopLeft().toBottomRight()
				.paintBorder(BorderStyle.fancy_light, BorderSpecification.INNER_VERTICAL).fromTopLeft().toBottomRight()
				.addHeaderBorder(BorderStyle.fancy_double).on(CellMatchers.row(0))
				.addAligner(SimpleVerticalAligner.middle).addAligner(SimpleHorizontalAligner.center);
		return Tables.configureKeyValueRendering(builder, " = ");
	}

	/**
	 * A formatter that collects bean property names and turns them into capitalized,
	 * separated words.
	 *
	 * @author Eric Bottard
	 */
	public static class BeanWrapperFormatter implements Formatter {

		private final Collection<String> includes;

		private final Collection<String> excludes;

		private final String delimiter;

		public BeanWrapperFormatter(String delimiter) {
			this(delimiter, null, Arrays.asList("class"));
		}

		public BeanWrapperFormatter(String delimiter, Collection<String> includes, Collection<String> excludes) {
			this.delimiter = delimiter;
			this.includes = includes;
			this.excludes = excludes;
		}

		@Override
		public String[] format(Object value) {
			if (value == null) {
				return new String[0];
			}
			else {
				BeanWrapper beanWrapper = new BeanWrapperImpl(value);
				return Arrays.stream(beanWrapper.getPropertyDescriptors()).map(PropertyDescriptor::getName).filter(
						n -> (includes == null || includes.contains(n)) && (excludes == null || !excludes.contains(n)))
						.map(n -> title(n) + delimiter + beanWrapper.getPropertyValue(n)).toArray(String[]::new);
			}
		}

		private String title(String n) {
			return Character.toUpperCase(n.charAt(0)) + n.substring(1).replaceAll("([A-Z])", " $1");
		}
	}
}
