/*
 * Copyright 2017 the original author or authors.
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

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;

import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

/**
 * A Hamcrest matcher to help with assertions on
 * {@link org.springframework.shell.table.Table}s without resorting to rendering them to a
 * String.
 *
 * @author Eric Bottard
 */
public class TableMatcher {

	public static DiagnosingMatcher<Table> hasRowThat(Matcher<?>... cells) {
		return new DiagnosingMatcher<Table>() {
			@Override
			protected boolean matches(Object item, Description mismatchDescription) {
				TableModel model = ((Table) item).getModel();
				outer: for (int row = 0; row < model.getRowCount(); row++) {
					mismatchDescription.appendText("\nRow " + row + ": ");
					for (int col = 0; col < cells.length; col++) {
						mismatchDescription.appendText("\n  Column " + col + ": ");
						cells[col].describeMismatch(model.getValue(row, col), mismatchDescription);
						if (!cells[col].matches(model.getValue(row, col))) {
							continue outer;
						}
					}
					return true;
				}
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("a table having at least one row that\n");
				for (int col = 0; col < cells.length; col++) {
					description.appendText("column " + col + ": ");
					cells[col].describeTo(description);
					description.appendText("\n");
				}
			}
		};
	}
}
