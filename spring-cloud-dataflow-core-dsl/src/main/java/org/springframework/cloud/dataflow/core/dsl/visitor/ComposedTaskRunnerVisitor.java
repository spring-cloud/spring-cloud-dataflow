/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.core.dsl.visitor;

import org.springframework.cloud.dataflow.core.dsl.SplitNode;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TaskVisitor;

/**
 * A visitor that counts the number of nodes in each split and records the high water mark.
 */
public class ComposedTaskRunnerVisitor extends TaskVisitor {

	private int highCount;
	private int currentCount;

	/**
	 * Reset current counter for a new split
	 */
	public void visit(SplitNode split) {
		currentCount = 0;

	}

	/**
	 * At end of split determine if the number of task apps in the split is greater than other splits.
	 */
	public void postVisit(SplitNode split) {
		if(currentCount > highCount) {
			highCount = currentCount;
		}
	}

	/**
	 * Count the number of tasks in a split.
	 */
	public void visit(TaskAppNode taskApp) {
		currentCount++;
	}

	public int getHighCount() {
		return highCount;
	}

	public void setHighCount(int highCount) {
		this.highCount = highCount;
	}

	public int getCurrentCount() {
		return currentCount;
	}

	public void setCurrentCount(int currentCount) {
		this.currentCount = currentCount;
	}
}
