/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.core.dsl;

import java.text.MessageFormat;

import static org.springframework.cloud.dataflow.core.dsl.DSLMessage.Kind.ERROR;

/**
 * Contains all the messages that can be produced during Spring Cloud Data Flow DSL
 * parsing. Each message has a kind (info, warn, error) and a code number. Tests can be
 * written to expect particular code numbers rather than particular text, enabling the
 * message text to more easily be modified and the tests to run successfully in different
 * locales.
 * <p>
 * When a message is formatted, it will have this kind of form
 * <p>
 *
 * <pre class="code">
 * 105E: (pos 34): Expected an argument value but was ' '
 * </pre>
 * <p>
 * </code> The prefix captures the code and the error kind, whilst the position is
 * included if it is known.
 *
 * @author Andy Clement
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public enum DSLMessage {

	UNEXPECTED_DATA_AFTER_STREAMDEF(ERROR, 100, "Found unexpected data after stream definition: ''{0}''"), //
	NO_WHITESPACE_BEFORE_ARG_NAME(ERROR, 101, "No whitespace allowed between '--' and option name"), //
	NO_WHITESPACE_BEFORE_ARG_EQUALS(ERROR, 102, "No whitespace allowed after argument name and before '='"), //
	NO_WHITESPACE_BEFORE_ARG_VALUE(ERROR, 103, "No whitespace allowed after '=' and before option value"), //
	MORE_INPUT(ERROR, 104, "After parsing a valid stream, there is still more data: ''{0}''"), //
	EXPECTED_ARGUMENT_VALUE(ERROR, 105, "Expected an argument value but was ''{0}''"), //
	NON_TERMINATING_DOUBLE_QUOTED_STRING(ERROR, 106, "Cannot find terminating \" for string"), //
	NON_TERMINATING_QUOTED_STRING(ERROR, 107, "Cannot find terminating '' for string"), //
	MISSING_CHARACTER(ERROR, 108, "missing expected character ''{0}''"), //
	NOT_EXPECTED_TOKEN(ERROR, 111, "Unexpected token.  Expected ''{0}'' but was ''{1}''"), //
	OOD(ERROR, 112, "Unexpectedly ran out of input"), //
	UNEXPECTED_ESCAPE_CHAR(ERROR, 114, "unexpected escape character."), //
	UNEXPECTED_DATA(ERROR, 115, "unexpected data in stream definition ''{0}''"), //
	UNRECOGNIZED_STREAM_REFERENCE(ERROR, 116, "unrecognized stream reference ''{0}''"), //
	UNRECOGNIZED_APP_REFERENCE(ERROR, 117, "unrecognized app reference ''{0}''"), //
	EXPECTED_APPNAME(ERROR, 118, "expected app name but found ''{0}''"), //
	EXPECTED_WHITESPACE_AFTER_APP_BEFORE_ARGUMENT(ERROR, 119, "expected whitespace after app name and before argument"), //
	ILLEGAL_STREAM_NAME(ERROR, 122, "illegal name for a stream ''{0}''"), //
	ILLEGAL_TASK_NAME(ERROR, 123, "illegal name for a task ''{0}''"), //
	MISSING_VALUE_FOR_VARIABLE(ERROR, 125, "no value specified for variable ''{0}'' when using substream"), //
	VARIABLE_NOT_TERMINATED(ERROR, 126, "unable to find variable terminator ''}'' in argument ''{0}''"), //
	AMBIGUOUS_APP_NAME(ERROR, 129,
			"ambiguous app name ''{0}'' in stream named ''{1}'', appears at both position {2} and {3}"), //
	CANNOT_USE_COMPOSEDAPP_HERE_AS_IT_DEFINES_SOURCE_DESTINATION(ERROR, 135,
			"cannot use composed app ''{0}'' here because it defines a source destination"), //
	CANNOT_USE_COMPOSEDAPP_HERE_AS_IT_DEFINES_SINK_DESTINATION(ERROR, 136,
			"cannot use composed app ''{0}'' here because it defines a sink destination"), //
	CANNOT_USE_COMPOSEDAPP_HERE_ALREADY_HAS_SOURCE_DESTINATION(ERROR, 137,
			"cannot use composed app ''{0}'' here, both that composed app and this stream define a source "
					+ "destination"), //
	CANNOT_USE_COMPOSEDAPP_HERE_ALREADY_HAS_SINK_DESTINATION(ERROR, 138,
			"cannot use composed app ''{0}'' here, both that composed app and this stream define a sink destination"), //
	EXPECTED_DESTINATION_PREFIX(ERROR, 133, "Expected destination prefix but found ''{0}''"), //
	NO_WHITESPACE_IN_DESTINATION_DEFINITION(ERROR, 139,
			"no whitespace allowed between components in a destination " + "name"), //
	NO_WHITESPACE_BETWEEN_LABEL_NAME_AND_COLON(ERROR, 140, "no whitespace allowed between label name and colon"), //
	DUPLICATE_LABEL(ERROR, 143,
			"label ''{0}'' should be unique but app ''{1}'' (at position {2}) and app ''{3}'' (at position {4}) both "
					+ "use it"), //
	APP_REFERENCE_NOT_UNIQUE(ERROR, 144,
			"reference to ''{0}'' is not unique in the target stream ''{1}'', please label the relevant app and use "
					+ "the label, or use a suffix index to indicate which occurrence of the app, e.g. ''{0}.0''"), //
	NO_WHITESPACE_IN_DOTTED_NAME(ERROR, 145, "no whitespace is allowed between dot and components of a name"),
	DESTINATIONS_UNSUPPORTED_HERE(ERROR, 146, "a destination is not supported in this kind of definition"), //
	EXPECTED_WHITESPACE_AFTER_LABEL_COLON(ERROR, 147, "whitespace is expected after an app label"), //
	EXPECTED_STREAM_NAME_AFTER_LABEL_COLON(ERROR, 148, "stream name is expected after an app label"), //
	// These relate specifically to the composed task DSL
	TASK_DOUBLE_AND_REQUIRED(ERROR, 150, "task dsl flow requires a pair of &, not just one"), //
	TASK_DOUBLE_OR_REQUIRED(ERROR, 151, "task dsl requires a pair of '|'"), //
	TASK_HYPHEN_EXPECTED_USE_FOR_TRANSITION(ERROR, 152, "expected '->' and not just the hyphen"), //
	TASK_MISSING_TRANSITION_ARROW(ERROR, 153, "expected '->' to follow state when specifying transition"), //
	TASK_UNQUOTED_TRANSITION_CHECK_MUST_BE_NUMBER(ERROR, 154,
			"transition conditions must be quoted literals, " + "numerics or '*' but ''{0}'' is not"), //
	TASK_NO_LABELS_ON_PARENS(ERROR, 155, "labels cannot be specified on parenthesized groups"), //
	TASK_NO_DOUBLE_LABELS(ERROR, 156, "double labels are not supported"), //
	TASK_ARROW_SHOULD_BE_PRECEDED_BY_CODE(ERROR, 157,
			"transition arrow must be preceeded by the exit code that " + "should drive the transition"), //
	TASK_VALIDATION_SECONDARY_SEQUENCES_MUST_BE_NAMED(ERROR, 158,
			"secondary sequences must have labels or are " + "unreachable"), //
	TASK_VALIDATION_DUPLICATE_LABEL(ERROR, 159, "this label has already been defined"), //
	TASK_VALIDATION_TRANSITION_TARGET_LABEL_UNDEFINED(ERROR, 160, "transition specifies an undefined label"), //
	TASK_ELEMENT_IN_COMPOSED_DEFINITION_DOES_NOT_EXIST(ERROR, 161, "task in composed task definition does not exist"), //
	TASK_UNEXPECTED_DATA(ERROR, 162, "unexpected data in task definition ''{0}''"), //
	TASK_MORE_INPUT(ERROR, 163, "after parsing a valid task, there is still more data: ''{0}''"), //
	TASK_VALIDATION_LABEL_CLASHES_WITH_TASKAPP_NAME(ERROR, 164,
			"the label clashes with an existing unlabeled task " + "application name"), //
	TASK_VALIDATION_APP_NAME_CLASHES_WITH_LABEL(ERROR, 165, "the app name clashes with an existing label"), //
	TASK_VALIDATION_APP_NAME_ALREADY_IN_USE(ERROR, 166, "duplicate app name. Use a label to ensure uniqueness"), //
	TASK_VALIDATION_SPLIT_WITH_ONE_FLOW(ERROR, 167,
			"unnecessary use of split construct when only one flow to execute" + " in parallel"), //
	TASK_ARGUMENTS_NOT_ALLOWED_UNLESS_IN_APP_MODE(ERROR, 168, "arguments not allowed unless parser is in app mode"), //
	;

	private Kind kind;

	private int code;

	private String message;

	DSLMessage(Kind kind, int code, String message) {
		this.kind = kind;
		this.code = code;
		this.message = message;
	}

	/**
	 * Produce a complete message including the prefix, the position (if known) and with
	 * the inserts applied to the message.
	 *
	 * @param pos the position, if less than zero it is ignored and not included in the
	 * message
	 * @param inserts the inserts to put into the formatted message
	 * @return a formatted message
	 */
	public String formatMessage(int pos, Object... inserts) {
		StringBuilder formattedMessage = new StringBuilder();
		formattedMessage.append(code);
		// switch (kind) {
		// case WARNING:
		// formattedMessage.append("W");
		// break;
		// case INFO:
		// formattedMessage.append("I");
		// break;
		// case ERROR:
		formattedMessage.append("E");
		// break;
		// }
		formattedMessage.append(":");
		if (pos != -1) {
			formattedMessage.append("(pos ").append(pos).append("): ");
		}
		formattedMessage.append(MessageFormat.format(message, inserts));
		return formattedMessage.toString();
	}

	public Kind getKind() {
		return kind;
	}

	public enum Kind {
		INFO,
		WARNING,
		ERROR
	}

}
