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

package org.springframework.cloud.dataflow.module.deployer.cloudfoundry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Helper class to run (several) closures that may fail, needing to be undone.
 *
 * @author Eric Bottard
 */
class Undoer {

	private List<Runnable> undos = new ArrayList<>();

	public <T> UndoerRunner<T> attempt(Callable<T> task) {
		return this.new UndoerRunner<T>(task);
	}

	public UndoerRunner<Void> attempt(final Runnable task) {
		return this.new UndoerRunner<Void>(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				task.run();
				return null;
			}
		});
	}

	public class UndoerRunner<T> {
		private final Callable<T> action;

		private UndoerRunner(Callable<T> action) {
			this.action = action;
		}

		public T andUndoBy(Runnable undoRunner) {
			try {
				T result = action.call();
				if (undoRunner != null) {
					undos.add(undoRunner);
				}
				return result;
			}
			catch (RuntimeException e) {
				undo();
				throw e;
			}
			catch (Exception e) {
				undo();
				throw new RuntimeException(e);
			}
		}

		public T withNoParticularUndo() {
			return andUndoBy(null);
		}
	}

	/**
	 * Attempt to undo all operations that have happened thus far. This will be called automatically
	 * if an operation wrapped in an {@link #attempt(Callable)} fails, but can also be called externally.
	 */
	public void undo() {
		Collections.reverse(undos);
		for (Runnable undo : undos) {
			try {
				undo.run();
			}
			catch (Exception e) {
				// ignored
			}
		}
	}

}
