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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andy Clement
 */
public class StreamDslParser {

	private String expressionString;

	private List<Token> tokenStream;

	private int tokenStreamLength;

	private int tokenStreamPointer; // Current location in the token stream when

	// processing tokens

	private int lastGoodPoint;

	/**
	 * Parse a stream definition without supplying the stream name up front.
	 * The stream name may be embedded in the definition.
	 * For example: <code>mystream = http | file</code>
	 *
	 * @return the AST for the parsed stream
	 */
	public StreamNode parse(String stream) {
		return parse(null, stream);
	}

	/**
	 * Parse a stream definition.
	 *
	 * @return the AST for the parsed stream
	 * @throws ParseException
	 */
	public StreamNode parse(String name, String stream) {
		this.expressionString = stream;
		Tokenizer tokenizer = new Tokenizer(expressionString);
		tokenStream = tokenizer.getTokens();
		tokenStreamLength = tokenStream.size();
		tokenStreamPointer = 0;
		StreamNode ast = eatStream();

		// Check the stream name, however it was specified
		if (ast.getName() != null && !isValidStreamName(ast.getName())) {
			throw new ParseException(ast.getName(), 0, DSLMessage.ILLEGAL_STREAM_NAME, ast.getName());
		}
		if (name != null && !isValidStreamName(name)) {
			throw new ParseException(name, 0, DSLMessage.ILLEGAL_STREAM_NAME, name);
		}

		// Check that each module has a unique label (either explicit or implicit)
		Map<String, ModuleNode> alreadySeen = new LinkedHashMap<String, ModuleNode>();
		for (int m = 0; m < ast.getModuleNodes().size(); m++) {
			ModuleNode node = ast.getModuleNodes().get(m);
			ModuleNode previous = alreadySeen.put(node.getLabelName(), node);
			if (previous != null) {
				String duplicate = node.getLabelName();
				int previousIndex = new ArrayList<String>(alreadySeen.keySet()).indexOf(duplicate);
				throw new ParseException(stream, node.startPos, DSLMessage.DUPLICATE_LABEL,
						duplicate, previous.getName(), previousIndex, node.getName(), m);
			}
		}

		// Check if the stream name is same as that of any of its modules' names
		// Can lead to infinite recursion during resolution, when parsing a composite module.
		if (ast.getModule(name) != null) {
			throw new ParseException(stream, stream.indexOf(name),
					DSLMessage.STREAM_NAME_MATCHING_MODULE_NAME,
					name);
		}
		if (moreTokens()) {
			throw new ParseException(this.expressionString, peekToken().startPos, DSLMessage.MORE_INPUT,
					toString(nextToken()));
		}

		//ast.resolve(this, this.expressionString); // todo

		return ast;
	}

	// (name =)
	private String maybeEatStreamName() {
		String streamName = null;
		if (lookAhead(1, TokenKind.EQUALS)) {
			if (peekToken(TokenKind.IDENTIFIER)) {
				streamName = eatToken(TokenKind.IDENTIFIER).data;
				nextToken(); // skip '='
			}
			else {
				raiseException(peekToken().startPos, DSLMessage.ILLEGAL_STREAM_NAME, toString(peekToken()));
			}
		}
		return streamName;
	}

	// stream: (streamName) (sourceChannel) moduleList (sinkChannel)
	private StreamNode eatStream() {
		String streamName = maybeEatStreamName();
		SourceChannelNode sourceChannelNode = maybeEatSourceChannel();
		// This construct: queue:foo > topic:bar is a source then a sink channel
		// with no module. Special handling for that is right here:
		boolean bridge = false;
		if (sourceChannelNode != null) { // so if we are just after a '>'
			if (looksLikeChannel() && noMorePipes()) {
				bridge = true;
			}
		}

		List<ModuleNode> moduleNodes = null;
		if (bridge) {
			// Create a bridge module to hang the source/sink channels off
			tokenStreamPointer--; // Rewind so we can nicely eat the sink channel
			moduleNodes = new ArrayList<ModuleNode>();
			moduleNodes.add(new ModuleNode(null, "bridge", peekToken().startPos, peekToken().endPos, null));
		}
		else {
			moduleNodes = eatModuleList();
		}
		SinkChannelNode sinkChannelNode = maybeEatSinkChannel();

		// Further data is an error
		if (moreTokens()) {
			Token t = peekToken();
			raiseException(t.startPos, DSLMessage.UNEXPECTED_DATA_AFTER_STREAMDEF, toString(t));
		}

		StreamNode streamNode = new StreamNode(expressionString, streamName, moduleNodes, sourceChannelNode,
				sinkChannelNode);
		return streamNode;
	}

	private boolean noMorePipes() {
		return noMorePipes(tokenStreamPointer);
	}

	private boolean noMorePipes(int tp) {
		while (tp < tokenStreamLength) {
			if (tokenStream.get(tp++).getKind() == TokenKind.PIPE) {
				return false;
			}
		}
		return true;
	}

	private boolean looksLikeChannel() {
		return looksLikeChannel(tokenStreamPointer);
	}

	enum ChannelPrefix {
		queue, tap, topic;
	}

	// return true if the specified tokenpointer appears to be pointing at a channel
	private boolean looksLikeChannel(int tp) {
		if (moreTokens() && tokenStream.get(tp).getKind() == TokenKind.IDENTIFIER) {
			String prefix = tokenStream.get(tp).data;
			if (isLegalChannelPrefix(prefix)) {
				if (tokenStreamPointer + 1 < tokenStream.size() && tokenStream.get(tp + 1).getKind() == TokenKind.COLON) {
					// if (isNextTokenAdjacent(tp) && isNextTokenAdjacent(tp + 1)) {
					return true;
					// }
				}
			}
		}
		return false;
	}

	// identifier ':' identifier >
	// tap ':' identifier ':' identifier '.' identifier >
	private SourceChannelNode maybeEatSourceChannel() {
		boolean gtBeforePipe = false;
		// Seek for a GT(>) before a PIPE(|)
		for (int tp = tokenStreamPointer; tp < tokenStreamLength; tp++) {
			Token t = tokenStream.get(tp);
			if (t.getKind() == TokenKind.GT) {
				gtBeforePipe = true;
				break;
			}
			else if (t.getKind() == TokenKind.PIPE) {
				break;
			}
		}
		if (!gtBeforePipe || !looksLikeChannel(tokenStreamPointer)) {
			return null;
		}

		ChannelNode channel = eatChannelReference(true);
		Token gt = eatToken(TokenKind.GT);
		return new SourceChannelNode(channel, gt.endPos);
	}

	// '>' identifier ':' identifier
	private SinkChannelNode maybeEatSinkChannel() {
		SinkChannelNode sinkChannelNode = null;
		if (peekToken(TokenKind.GT)) {
			Token gt = eatToken(TokenKind.GT);
			ChannelNode channelNode = eatChannelReference(false);
			sinkChannelNode = new SinkChannelNode(channelNode, gt.startPos);
		}
		return sinkChannelNode;
	}

	private boolean isLegalChannelPrefix(String string) {
		return string.equals(ChannelPrefix.queue.toString()) ||
				string.equals(ChannelPrefix.topic.toString()) ||
				string.equals(ChannelPrefix.tap.toString());
	}

	// A channel reference is a colon separated list of identifiers that determine
	// the appropriate scope then a sequence of dot separated identifiers that
	// reference something within that scope.
	// Only three types of top level prefix are supported for channels:queue, topic, tap
	// identifier [ ':' identifier ]* [ '.' identifier ]*
	// If the first identifier is a tap (and tapping is allowed) then
	// the dereferencing is allowed
	private ChannelNode eatChannelReference(boolean tapAllowed) {
		Token firstToken = nextToken();
		if (!firstToken.isIdentifier() || !isLegalChannelPrefix(firstToken.data)) {
			raiseException(firstToken.startPos,
					tapAllowed ? DSLMessage.EXPECTED_CHANNEL_PREFIX_QUEUE_TOPIC_TAP
							: DSLMessage.EXPECTED_CHANNEL_PREFIX_QUEUE_TOPIC,
					toString(firstToken));
		}
		List<Token> channelScopeComponents = new ArrayList<Token>();
		channelScopeComponents.add(firstToken);
		while (peekToken(TokenKind.COLON)) {
			if (!isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_IN_CHANNEL_DEFINITION);
			}
			nextToken(); // skip colon
			if (!isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_IN_CHANNEL_DEFINITION);
			}
			channelScopeComponents.add(eatToken(TokenKind.IDENTIFIER));
		}
		List<Token> channelReferenceComponents = new ArrayList<Token>();
		if (tapAllowed && firstToken.data.equalsIgnoreCase("tap")) {
			if (peekToken(TokenKind.DOT)) {
				if (channelScopeComponents.size() < 3) {
					raiseException(firstToken.startPos, DSLMessage.TAP_NEEDS_THREE_COMPONENTS);
				}
				String tokenData = channelScopeComponents.get(1).data;
				// for Stream, tap:stream:XXX - the channel name is always indexed
				// for Job, tap:job:XXX - the channel name can have "." in case of job notification channels
				if (!tokenData.equalsIgnoreCase("stream") && !tokenData.equalsIgnoreCase("job")) {
					raiseException(peekToken().startPos, DSLMessage.ONLY_A_TAP_ON_A_STREAM_OR_JOB_CAN_BE_INDEXED);
				}
			}
			while (peekToken(TokenKind.DOT)) {
				if (!isNextTokenAdjacent()) {
					raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_IN_CHANNEL_DEFINITION);
				}
				nextToken(); // skip dot
				if (!isNextTokenAdjacent()) {
					raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_IN_CHANNEL_DEFINITION);
				}
				channelReferenceComponents.add(eatToken(TokenKind.IDENTIFIER));
			}
		}
		else if (peekToken(TokenKind.DOT)) {
			if (tapAllowed) {
				raiseException(peekToken().startPos, DSLMessage.ONLY_A_TAP_ON_A_STREAM_OR_JOB_CAN_BE_INDEXED);
			}
			else {
				raiseException(peekToken().startPos, DSLMessage.CHANNEL_INDEXING_NOT_ALLOWED);
			}
		}
		// Verify the structure:
		ChannelType channelType = null;
		if (firstToken.data.equalsIgnoreCase("tap")) {
			// tap:stream:XXX.YYY
			// tap:job:XXX
			// tap:queue:XXX
			// tap:topic:XXX
			if (channelScopeComponents.size() < 3) {
				raiseException(firstToken.startPos, DSLMessage.TAP_NEEDS_THREE_COMPONENTS);
			}
			Token tappingToken = channelScopeComponents.get(1);
			String tapping = tappingToken.data.toLowerCase();
			channelScopeComponents.remove(0); // remove 'tap'
			if (tapping.equals("stream")) {
				channelType = ChannelType.TAP_STREAM;
			}
			else if (tapping.equals("job")) {
				channelType = ChannelType.TAP_JOB;
			}
			else if (tapping.equals("queue")) {
				channelType = ChannelType.TAP_QUEUE;
			}
			else if (tapping.equals("topic")) {
				channelType = ChannelType.TAP_TOPIC;
			}
			else {
				raiseException(tappingToken.startPos, DSLMessage.NOT_ALLOWED_TO_TAP_THAT, tappingToken.data);
			}
		}
		else {
			// queue:XXX
			// topic:XXX
			if (firstToken.data.equalsIgnoreCase("queue")) {
				channelType = ChannelType.QUEUE;
			}
			else if (firstToken.data.equalsIgnoreCase("topic")) {
				channelType = ChannelType.TOPIC;
			}
			// TODO: DT not sure if this is the best way to handle
			// StreamConfigParserTests.substreamsWithSourceChannels()
			if (channelScopeComponents.size() >= 3) {
				channelScopeComponents.remove(0);
			}
		}
		int endpos = channelScopeComponents.get(channelScopeComponents.size() - 1).endPos;
		if (!channelReferenceComponents.isEmpty()) {
			endpos = channelReferenceComponents.get(channelReferenceComponents.size() - 1).endPos;
		}
		return new ChannelNode(channelType, firstToken.startPos, endpos, tokenListToStringList(channelScopeComponents),
				tokenListToStringList(channelReferenceComponents));
	}

	private List<String> tokenListToStringList(List<Token> tokens) {
		if (tokens.isEmpty()) {
			return Collections.<String> emptyList();
		}
		List<String> data = new ArrayList<String>();
		for (Token token : tokens) {
			data.add(token.data);
		}
		return data;
	}

	// moduleList: module (| module)*
	// A stream may end in a module (if it is a sink) or be followed by
	// a sink channel.
	private List<ModuleNode> eatModuleList() {
		List<ModuleNode> moduleNodes = new ArrayList<ModuleNode>();

		moduleNodes.add(eatModule());
		while (moreTokens()) {
			Token t = peekToken();
			if (t.kind == TokenKind.PIPE) {
				nextToken();
				moduleNodes.add(eatModule());
			}
			else {
				// might be followed by sink channel
				break;
			}
		}
		return moduleNodes;
	}

	// module: [label':']? identifier (moduleArguments)*
	private ModuleNode eatModule() {
		Token label = null;
		Token name = nextToken();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			raiseException(name.startPos, DSLMessage.EXPECTED_MODULENAME, name.data != null ? name.data
					: new String(name.getKind().tokenChars));
		}
		if (peekToken(TokenKind.COLON)) {
			if (!isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_BETWEEN_LABEL_NAME_AND_COLON);
			}
			nextToken(); // swallow colon
			label = name;
			name = eatToken(TokenKind.IDENTIFIER);
		}
		Token moduleName = name;
		checkpoint();
		ArgumentNode[] args = maybeEatModuleArgs();
		int startpos = label != null ? label.startPos : moduleName.startPos;
		return new ModuleNode(toLabelNode(label), moduleName.data, startpos, moduleName.endPos, args);
	}

	private LabelNode toLabelNode(Token label) {
		if (label == null) {
			return null;
		}
		return new LabelNode(label.data, label.startPos, label.endPos);
	}

	// moduleArguments : DOUBLE_MINUS identifier(name) EQUALS identifier(value)
	private ArgumentNode[] maybeEatModuleArgs() {
		List<ArgumentNode> args = null;
		if (peekToken(TokenKind.DOUBLE_MINUS) && isNextTokenAdjacent()) {
			raiseException(peekToken().startPos, DSLMessage.EXPECTED_WHITESPACE_AFTER_MODULE_BEFORE_ARGUMENT);
		}
		while (peekToken(TokenKind.DOUBLE_MINUS)) {
			Token dashDash = nextToken(); // skip the '--'
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_BEFORE_ARG_NAME);
			}
			List<Token> argNameComponents = eatDottedName();
			if (peekToken(TokenKind.EQUALS) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_BEFORE_ARG_EQUALS);
			}
			eatToken(TokenKind.EQUALS);
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_BEFORE_ARG_VALUE);
			}
			// Process argument value:
			Token t = peekToken();
			String argValue = eatArgValue();
			checkpoint();
			if (args == null) {
				args = new ArrayList<ArgumentNode>();
			}
			args.add(new ArgumentNode(data(argNameComponents), argValue, dashDash.startPos, t.endPos));
		}
		return args == null ? null : args.toArray(new ArgumentNode[args.size()]);
	}

	// argValue: identifier | literal_string
	private String eatArgValue() {
		Token t = nextToken();
		String argValue = null;
		if (t.getKind() == TokenKind.IDENTIFIER) {
			argValue = t.data;
		}
		else if (t.getKind() == TokenKind.LITERAL_STRING) {
			String quotesUsed = t.data.substring(0, 1);
			argValue = t.data.substring(1, t.data.length() - 1).replace(quotesUsed+quotesUsed, quotesUsed);
		}
		else {
			raiseException(t.startPos, DSLMessage.EXPECTED_ARGUMENT_VALUE, t.data);
		}
		return argValue;
	}

	private Token eatToken(TokenKind expectedKind) {
		Token t = nextToken();
		if (t == null) {
			raiseException(expressionString.length(), DSLMessage.OOD);
		}
		if (t.kind != expectedKind) {
			raiseException(t.startPos, DSLMessage.NOT_EXPECTED_TOKEN, expectedKind.toString().toLowerCase(),
					t.getKind().toString().toLowerCase() + (t.data == null ? "" : "(" + t.data + ")"));
		}
		return t;
	}

	private boolean peekToken(TokenKind desiredTokenKind) {
		return peekToken(desiredTokenKind, false);
	}

	private boolean lookAhead(int distance, TokenKind desiredTokenKind) {
		if ((tokenStreamPointer + distance) >= tokenStream.size()) {
			return false;
		}
		Token t = tokenStream.get(tokenStreamPointer + distance);
		if (t.kind == desiredTokenKind) {
			return true;
		}
		return false;
	}

	private List<Token> eatDottedName() {
		return eatDottedName(DSLMessage.NOT_EXPECTED_TOKEN);
	}

	/**
	 * Consumes and returns (identifier [DOT identifier]*) as long as they're adjacent.
	 *
	 * @param error the kind of error to report if input is ill-formed
	 */
	private List<Token> eatDottedName(DSLMessage error) {
		List<Token> result = new ArrayList<Token>(3);
		Token name = nextToken();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			raiseException(name.startPos, error, name.data != null ? name.data
					: new String(name.getKind().tokenChars));
		}
		result.add(name);
		while (peekToken(TokenKind.DOT)) {
			if (!isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME);
			}
			result.add(nextToken()); // consume dot
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME);
			}
			result.add(eatToken(TokenKind.IDENTIFIER));
		}
		return result;
	}

	/**
	 * Verify the supplied name is a valid stream name. Valid stream names must follow the same rules as java
	 * identifiers, with the additional option to use a hyphen ('-') after the first character.
	 *
	 * @param streamname the name to validate
	 * @return true if name is valid
	 */
	public static boolean isValidStreamName(String streamname) {
		if (streamname.length() == 0) {
			return false;
		}
		if (!Character.isJavaIdentifierStart(streamname.charAt(0))) {
			return false;
		}
		for (int i = 1, max = streamname.length(); i < max; i++) {
			char ch = streamname.charAt(i);
			if (!(Character.isJavaIdentifierPart(ch) || ch == '-')) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return the concatenation of the data of many tokens.
	 */
	private String data(Iterable<Token> many) {
		StringBuilder result = new StringBuilder();
		for (Token t : many) {
			if (t.getKind().hasPayload()) {
				result.append(t.data);
			}
			else {
				result.append(t.getKind().tokenChars);
			}
		}
		return result.toString();
	}

	private boolean peekToken(TokenKind desiredTokenKind, boolean consumeIfMatched) {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		if (t.kind == desiredTokenKind) {
			if (consumeIfMatched) {
				tokenStreamPointer++;
			}
			return true;
		}
		else {
			return false;
		}
	}

	private boolean moreTokens() {
		return tokenStreamPointer < tokenStream.size();
	}

	private Token nextToken() {
		if (tokenStreamPointer >= tokenStreamLength) {
			raiseException(expressionString.length(), DSLMessage.OOD);
		}
		return tokenStream.get(tokenStreamPointer++);
	}

	private boolean isNextTokenAdjacent() {
		if (tokenStreamPointer >= tokenStreamLength) {
			return false;
		}
		Token last = tokenStream.get(tokenStreamPointer - 1);
		Token next = tokenStream.get(tokenStreamPointer);
		return next.startPos == last.endPos;
	}

	private Token peekToken() {
		if (tokenStreamPointer >= tokenStreamLength) {
			return null;
		}
		return tokenStream.get(tokenStreamPointer);
	}

	private void raiseException(int pos, DSLMessage message, Object... inserts) {
		throw new CheckPointedParseException(expressionString, pos, tokenStreamPointer, lastGoodPoint,
				tokenStream, message, inserts);
	}

	private void checkpoint() {
		lastGoodPoint = tokenStreamPointer;
	}

	private String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		}
		else {
			return new String(t.kind.getTokenChars());
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(tokenStream).append("\n");
		s.append("tokenStreamPointer=" + tokenStreamPointer).append("\n");
		return s.toString();
	}

}
