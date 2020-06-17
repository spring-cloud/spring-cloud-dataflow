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

package org.springframework.cloud.dataflow.completion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.util.Assert;

/**
 * A recovery strategy that will trigger if the parser failure is similar to that of some
 * sample unfinished stream definition. The match is decided by analyzing the top frames
 * of the stack trace emitted by the parser when it encounters the ill formed input.
 * Multiple fingerprints are supported, as the control flow in the parser code may be
 * different depending on the form of the expression. For example, for the rule
 * {@code stream = module (| module)* }, the pseudo code for the parser may look like
 * <pre>
 * {@code
 * stream() {
 *   module();  (1)
 *   while(moreInput()) {
 *     swallowPipe();
 *     module();  (2)
 *   }
 * }
 * }
 * </pre>
 * In that setup, whether we're dealing with the first module, or a subsequent module,
 * stack frames would be different (see (1) and (2)).
 *
 * @author Eric Bottard
 */
public abstract class StacktraceFingerprintingRecoveryStrategy<E extends Exception> implements RecoveryStrategy<E> {

	private final Set<List<StackTraceElement>> fingerprints = new LinkedHashSet<>();

	private final Class<E> exceptionClass;

	protected StreamDefinitionService streamDefinitionService;

	/**
	 * Construct a new StacktraceFingerprintingRecoveryStrategy given the parser, and the
	 * expected exception class to be thrown for sample fragments of a stream definition
	 * that is to be parsed.
	 *
	 * @param exceptionClass the expected exception that results from parsing the sample
	 * fragment stream definitions. Stack frames from the thrown exception are used to
	 * store the fingerprint of this exception thrown by the parser.
	 * @param streamDefinitionService the service to handle stream definition
	 * @param samples the sample fragments of stream definitions.
	 */
	public StacktraceFingerprintingRecoveryStrategy(Class<E> exceptionClass, StreamDefinitionService streamDefinitionService, String... samples) {
		Assert.notNull(exceptionClass, "exceptionClass should not be null");
		Assert.notNull(streamDefinitionService, "streamDefinitionService should not be null");
		Assert.notEmpty(samples, "samples should not be null or empty");
		this.exceptionClass = exceptionClass;
		this.streamDefinitionService = streamDefinitionService;
		initFingerprints(samples);
	}

	@SuppressWarnings("unchecked")
	private void initFingerprints(String... samples) {
		for (String sample : samples) {
			try {
				this.streamDefinitionService.parse(new StreamDefinition("__dummy", sample));
			}
			catch (RuntimeException exception) {
				if (this.exceptionClass.isAssignableFrom(exception.getClass())) {
					addFingerprintForException((E) exception);
				}
				else {
					throw exception;
				}
			}
		}
	}

	/**
	 * Extract the top frames (until the call to the {@link StreamDefinition} constructor
	 * appears) of the given exception.
	 */
	private void addFingerprintForException(E exception) {
		boolean seenParserClass = false;
		List<StackTraceElement> fingerPrint = new ArrayList<StackTraceElement>();
		for (StackTraceElement frame : exception.getStackTrace()) {
			if (frame.getClassName().equals(StreamDefinition.class.getName())) {
				seenParserClass = true;
			}
			else if (seenParserClass) {
				break;
			}
			fingerPrint.add(frame);
		}
		fingerprints.add(fingerPrint);
	}

	private boolean fingerprintMatches(E exception, List<StackTraceElement> fingerPrint) {
		int i = 0;
		StackTraceElement[] stackTrace = exception.getStackTrace();
		for (StackTraceElement frame : fingerPrint) {
			if (frame.getClassName().contains("StreamParser")
					&& (frame.getMethodName().equals("<init>") || frame.getMethodName().equals("parse"))) {
				return true;
			}
			if (!stackTrace[i++].equals(frame)) {
				return false;
			}
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean shouldTrigger(String dslStart, Exception exception) {
		if (!exceptionClass.isAssignableFrom(exception.getClass())) {
			return false;
		}
		for (List<StackTraceElement> fingerPrint : fingerprints) {
			if (fingerprintMatches((E) exception, fingerPrint)) {
				return true;
			}
		}
		return false;
	}

}
