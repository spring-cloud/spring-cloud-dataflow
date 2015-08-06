/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.core.dsl;

import static org.springframework.cloud.data.core.dsl.DSLMessage.Kind.ERROR;

import java.text.MessageFormat;

/**
 * Contains all the messages that can be produced during Spring Cloud Data DSL
 * parsing. Each message has a kind (info, warn, error) and a code number.
 * Tests can be written to expect particular code numbers rather than
 * particular text, enabling the message text to more easily be modified
 * and the tests to run successfully in different locales.
 * <p>
 * When a message is formatted, it will have this kind of form
 *
 * <pre class="code">
 * 105E: (pos 34): Expected an argument value but was ' '
 * </pre>
 *
 * </code> The prefix captures the code and the error kind, whilst the position is included if it is known.
 *
 * @author Andy Clement
 */
public enum DSLMessage {

	UNEXPECTED_DATA_AFTER_STREAMDEF(ERROR, 100, "Found unexpected data after stream definition: ''{0}''"), //
	NO_WHITESPACE_BEFORE_ARG_NAME(ERROR, 101, "No whitespace allowed between '--' and option name"), //
	NO_WHITESPACE_BEFORE_ARG_EQUALS(ERROR, 102, "No whitespace allowed after argument name and before '='"), //
	NO_WHITESPACE_BEFORE_ARG_VALUE(ERROR, 103, "No whitespace allowed after '=' and before option value"), //
	MORE_INPUT(ERROR, 104, "After parsing a valid stream, there is still more data: ''{0}''"), EXPECTED_ARGUMENT_VALUE(
			ERROR, 105, "Expected an argument value but was ''{0}''"), NON_TERMINATING_DOUBLE_QUOTED_STRING(ERROR, 106,
			"Cannot find terminating \" for string"), //
	NON_TERMINATING_QUOTED_STRING(ERROR, 107, "Cannot find terminating '' for string"), //
	MISSING_CHARACTER(ERROR, 108, "missing expected character ''{0}''"), NOT_EXPECTED_TOKEN(ERROR, 111,
			"Unexpected token.  Expected ''{0}'' but was ''{1}''"), OOD(ERROR, 112, "Unexpectedly ran out of input"), //
	UNEXPECTED_ESCAPE_CHAR(ERROR, 114, "unexpected escape character."), //
	UNEXPECTED_DATA(ERROR, 115, "unexpected data in stream definition ''{0}''"), //
	UNRECOGNIZED_STREAM_REFERENCE(ERROR, 116, "unrecognized stream reference ''{0}''"), //
	UNRECOGNIZED_MODULE_REFERENCE(ERROR, 117, "unrecognized module reference ''{0}''"), //
	EXPECTED_MODULENAME(ERROR, 118, "expected module name but found ''{0}''"), //
	EXPECTED_WHITESPACE_AFTER_MODULE_BEFORE_ARGUMENT(ERROR, 119,
			"expected whitespace after module name and before argument"), //
	EXPECTED_CHANNEL_QUALIFIER(ERROR, 120, "expected channel reference '':<channel>'' but found ''{0}''"), //
	EXPECTED_CHANNEL_NAME(ERROR, 121, "expected channel name but found ''{0}''"), //
	ILLEGAL_STREAM_NAME(ERROR, 122, "illegal name for a stream ''{0}''"), //
	ILLEGAL_TASK_NAME(ERROR, 123, "illegal name for a task ''{0}''"), //
	MISSING_VALUE_FOR_VARIABLE(ERROR, 125, "no value specified for variable ''{0}'' when using substream"), //
	VARIABLE_NOT_TERMINATED(ERROR, 126, "unable to find variable terminator ''}'' in argument ''{0}''"), //
	AMBIGUOUS_MODULE_NAME(ERROR,
			129,
			"ambiguous module name ''{0}'' in stream named ''{1}'', appears at both position {2} and {3}"), //
	STREAM_NAME_MATCHING_MODULE_NAME(ERROR, 130,
			"Stream name ''{0}'' same as that of its modules' names is not allowed."), //
	CHANNEL_INDEXING_NOT_ALLOWED(ERROR, 131, "Channel does not support indexing"), //
	ONLY_A_TAP_ON_A_STREAM_OR_JOB_CAN_BE_INDEXED(ERROR, 132, "Only a tap on a stream or job can be indexed"), //
	EXPECTED_CHANNEL_PREFIX_QUEUE_TOPIC(ERROR,
			133,
			"Expected channel prefix of ''queue'' or ''topic'' but found ''{0}''"), //
	EXPECTED_CHANNEL_PREFIX_QUEUE_TOPIC_TAP(ERROR,
			134,
			"Expected channel prefix of ''queue'', ''topic'' or ''tap'' but found ''{0}''"), //
	CANNOT_USE_COMPOSEDMODULE_HERE_AS_IT_DEFINES_SOURCE_CHANNEL(ERROR,
			135,
			"cannot use composed module ''{0}'' here because it defines a source channel"), //
	CANNOT_USE_COMPOSEDMODULE_HERE_AS_IT_DEFINES_SINK_CHANNEL(ERROR,
			136,
			"cannot use composed module ''{0}'' here because it defines a sink channel"), //
	CANNOT_USE_COMPOSEDMODULE_HERE_ALREADY_HAS_SOURCE_CHANNEL(ERROR,
			137,
			"cannot use composed module ''{0}'' here, both that composed module and this stream define a source channel"), //
	CANNOT_USE_COMPOSEDMODULE_HERE_ALREADY_HAS_SINK_CHANNEL(ERROR,
			138,
			"cannot use composed module ''{0}'' here, both that composed module and this stream define a sink channel"), //
	NO_WHITESPACE_IN_CHANNEL_DEFINITION(ERROR, 139, "no whitespace allowed between components in a channel name"), //
	NO_WHITESPACE_BETWEEN_LABEL_NAME_AND_COLON(ERROR, 140, "no whitespace allowed between label name and colon"), //
	TAP_NEEDS_THREE_COMPONENTS(ERROR,
			141,
			"a tap needs at least three components (tap:queue:[name], tap:topic:[name], tap:stream:[streamname] or tap:job:[jobname])"), //
	NOT_ALLOWED_TO_TAP_THAT(ERROR,
			142,
			"tapping only allowed on 'stream', 'topic', 'job' or 'queue'. Not allowed on ''{0}''"), //
	DUPLICATE_LABEL(ERROR,
			143,
			"Label ''{0}'' should be unique but module ''{1}'' (at position {2}) and module ''{3}'' (at position {4}) both use it"), //
	MODULE_REFERENCE_NOT_UNIQUE(ERROR,
			144,
			"Reference to ''{0}'' is not unique in the target stream ''{1}'', please label the relevant module and use the label, or use a suffix index to indicate which occurrence of the module, e.g. ''{0}.0''"), //
	NO_WHITESPACE_IN_DOTTED_NAME(ERROR,
			145,
			"No whitespace is allowed between dot and components of a name"),
	NAMED_CHANNELS_UNSUPPORTED_HERE(ERROR, 146, "A named channel is not supported in this kind of definition"), ;

	private Kind kind;

	private int code;

	private String message;

	private DSLMessage(Kind kind, int code, String message) {
		this.kind = kind;
		this.code = code;
		this.message = message;
	}

	/**
	 * Produce a complete message including the prefix, the position (if known) and with the inserts applied to the
	 * message.
	 *
	 * @param pos the position, if less than zero it is ignored and not included in the message
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

	public static enum Kind {
		INFO, WARNING, ERROR
	}

}