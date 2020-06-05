/*
 * Copyright 2015-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for stream DSL that generates {@link StreamNode}.
 *
 * @author Andy Clement
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class StreamParser extends AppParser {

	/**
	 * Stream name (may be {@code null}).
	 */
	private final String name;

	/**
	 * Stream DSL text.
	 */
	private final String dsl;

	/**
	 * Construct a {@code StreamParser} without supplying the stream name up front. The
	 * stream name may be embedded in the definition; for example:
	 * {@code mystream = http | file}.
	 *
	 * @param dsl the stream definition DSL text
	 */
	public StreamParser(String dsl) {
		this(null, dsl);
	}

	/**
	 * Construct a {@code StreamParser} for a stream with the provided name.
	 *
	 * @param name stream name
	 * @param dsl stream dsl text
	 */
	public StreamParser(String name, String dsl) {
		super(new Tokenizer().getTokens(dsl));
		this.name = name;
		this.dsl = dsl;
	}

	/**
	 * Parse a stream definition.
	 *
	 * @return the AST for the parsed stream
	 * @throws ParseException thrown if expression fails parsing.
	 */
	public StreamNode parse() {
		StreamNode ast = eatStream();

		// Check the stream name, however it was specified
		if (ast.getName() != null && !isValidName(ast.getName())) {
			throw new ParseException(ast.getName(), 0, DSLMessage.ILLEGAL_STREAM_NAME, ast.getName());
		}
		if (name != null && !isValidName(name)) {
			throw new ParseException(name, 0, DSLMessage.ILLEGAL_STREAM_NAME, name);
		}

		// Check that each app has a unique label (either explicit or implicit)
		Map<String, AppNode> alreadySeen = new LinkedHashMap<String, AppNode>();
		for (int m = 0; m < ast.getAppNodes().size(); m++) {
			AppNode node = ast.getAppNodes().get(m);
			AppNode previous = alreadySeen.put(node.getLabelName(), node);
			if (previous != null) {
				String duplicate = node.getLabelName();
				int previousIndex = new ArrayList<String>(alreadySeen.keySet()).indexOf(duplicate);
				throw new ParseException(dsl, node.startPos, DSLMessage.DUPLICATE_LABEL, duplicate, previous.getName(),
						previousIndex, node.getName(), m);
			}
		}
		Tokens tokens = getTokens();
		if (tokens.hasNext()) {
			tokens.raiseException(tokens.peek().startPos, DSLMessage.MORE_INPUT, toString(tokens.next()));
		}

		return ast;
	}

	/**
	 * If a stream name is present, return it and advance the token position - otherwise
	 * return {@code null}.
	 * <p>
	 * Expected format: {@code name =}
	 *
	 * @return stream name if present
	 */
	protected String eatStreamName() {
		Tokens tokens = getTokens();
		String streamName = null;
		if (tokens.lookAhead(1, TokenKind.EQUALS)) {
			if (tokens.peek(TokenKind.IDENTIFIER)) {
				streamName = tokens.eat(TokenKind.IDENTIFIER).data;
				tokens.next(); // skip '='
			}
			else {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.ILLEGAL_STREAM_NAME, toString(tokens.peek()));
			}
		}
		return streamName;
	}

	/**
	 * Return a {@link StreamNode} based on the tokens resulting from the parsed DSL.
	 * <p>
	 * Expected format:
	 * {@code stream: (streamName) (sourceDestination) appList (sinkDestination)}
	 *
	 * @return {@code StreamNode} based on parsed DSL
	 */
	protected StreamNode eatStream() {
		String streamName = eatStreamName();
		SourceDestinationNode sourceDestinationNode = eatSourceDestination();
		// This construct: :foo > :bar is a source then a sink destination
		// with no app. Special handling for that is right here:
		boolean bridge = false;
		if (sourceDestinationNode != null) { // so if we are just after a '>'
			if (looksLikeDestination() && noMorePipes()) {
				bridge = true;
			}
		}
		Tokens tokens = getTokens();
		List<AppNode> appNodes = new ArrayList<>();
		if (bridge) {
			// Create a bridge app to hang the source/sink destinations off
			tokens.decrementPosition(); // Rewind so we can nicely eat the sink destination
			AppNode bridgeAppNode = makeAppNode(null, "bridge", tokens.peek().startPos, tokens.peek().endPos, null);
			bridgeAppNode.setUnboundStreamApp(false);
			appNodes.add(bridgeAppNode);
		}
		else {
			appNodes.addAll(eatAppList(sourceDestinationNode!=null));
		}
		SinkDestinationNode sinkDestinationNode = eatSinkDestination();
		
		// Further data is an error
		if (tokens.hasNext()) {
			Token t = tokens.peek();
			DSLMessage errorMessage = DSLMessage.UNEXPECTED_DATA_AFTER_STREAMDEF;
			if (!appNodes.isEmpty() && sinkDestinationNode == null
					&& tokens.getTokenStream().get(tokens.position() - 1).isKind(TokenKind.GT)) {
				// Additional token where a destination is expected, but has no prefix
				errorMessage = DSLMessage.EXPECTED_DESTINATION_PREFIX;
			}
			tokens.raiseException(t.startPos, errorMessage, toString(t));
		}
		return makeStreamNode(tokens.getExpression(), streamName, appNodes, sourceDestinationNode, sinkDestinationNode);
	}

	/**
	 * Return {@code true} if no more pipes are present from the current token position.
	 *
	 * @return {@code true} if no more pipes are present from the current token position
	 */
	protected boolean noMorePipes() {
		return noMorePipes(getTokens().position());
	}

	/**
	 * Return {@code true} if no more pipes are present from the given position.
	 *
	 * @param position token position from which to check for the presence of pipes
	 * @return {@code true} if no more pipes are present from the given position
	 */
	protected boolean noMorePipes(int position) {
		List<Token> tokenList = getTokens().getTokenStream();
		int tokenStreamLength = tokenList.size();
		while (position < tokenStreamLength) {
			if (tokenList.get(position++).getKind() == TokenKind.PIPE) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return {@code true} if the current token position appears to be pointing at a
	 * destination.
	 *
	 * @return {@code true} if the current token position appears to be pointing at a
	 * destination
	 */
	protected boolean looksLikeDestination() {
		return looksLikeDestination(getTokens().position());
	}

	/**
	 * Return {@code true} if the indicated position appears to be pointing at a
	 * destination.
	 *
	 * @param position token position to check
	 * @return {@code true} if the indicated position appears to be pointing at a
	 * destination.
	 */
	protected boolean looksLikeDestination(int position) {
		Tokens tokens = getTokens();
		List<Token> tokenList = tokens.getTokenStream();
		if (tokens.hasNext() && tokenList.get(position).getKind() == TokenKind.COLON) {
			if (tokenList.get(position - 1).isKind(TokenKind.GT)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * If the current token position contains a source destination, return a
	 * {@link SourceDestinationNode} and advance the token position; otherwise return
	 * {@code null}.
	 * <p>
	 * Expected format: {@code ':' identifier >} {@code ':' identifier '.' identifier >}
	 *
	 * @return a {@code SourceDestinationNode} or {@code null} if the token position is
	 * not pointing at a source destination
	 */
	protected SourceDestinationNode eatSourceDestination() {
		Tokens tokens = getTokens();
		boolean gtBeforePipe = false;
		// Seek for a GT(>) before a PIPE(|)
		List<Token> tokenList = tokens.getTokenStream();
		for (int i = tokens.position(); i < tokenList.size(); i++) {
			Token t = tokenList.get(i);
			if (t.getKind() == TokenKind.GT) {
				gtBeforePipe = true;
				break;
			}
			else if (t.getKind() == TokenKind.PIPE) {
				break;
			}
		}
		if (!gtBeforePipe) {
			return null;
		}

		DestinationNode destinationNode = eatDestinationReference(false);
		if (destinationNode == null) {
			return null;
		}
		Token gt = tokens.eat(TokenKind.GT);
		return new SourceDestinationNode(destinationNode, gt.endPos);
	}

	/**
	 * If the current token position contains a sink destination, return a
	 * {@link SinkDestinationNode} and advance the token position; otherwise return
	 * {@code null}.
	 * <p>
	 * Expected format: {@code '>' ':' identifier}
	 *
	 * @return a {@code SinkDestinationNode} or {@code null} if the token position is not
	 * pointing at a sink destination
	 */
	protected SinkDestinationNode eatSinkDestination() {
		Tokens tokens = getTokens();
		SinkDestinationNode SinkDestinationNode = null;
		if (tokens.peek(TokenKind.GT)) {
			Token gt = tokens.eat(TokenKind.GT);
			DestinationNode destinationNode = eatDestinationReference(false);
			if (destinationNode == null) {
				return null;
			}
			SinkDestinationNode = new SinkDestinationNode(destinationNode, gt.startPos);
		}
		return SinkDestinationNode;
	}

	protected Token peekDestinationComponentToken() {
		Token t = getTokens().peek();
		if (t == null) {
			return null;
		}
		if (t.kind == TokenKind.IDENTIFIER || t.kind == TokenKind.STAR || t.kind == TokenKind.SLASH
				|| t.kind == TokenKind.HASH) {
			return t;
		}
		else {
			return null;
		}
	}

	protected String getTokenData(Token token) {
		return token.kind.hasPayload() ? token.data : new String(token.kind.getTokenChars());
	}

	/**
	 * Return a {@link DestinationNode} for the token at the current position.
	 * <p>
	 * A destination reference is the label component when referencing a specific
	 * app/label in a stream definition.
	 * <p>
	 * Expected format: {@code ':' identifier [ '.' identifier ]*}
	 * <p>
	 *
	 * @return {@code DestinationNode} representing the destination reference
	 */
	protected DestinationNode eatDestinationReference(boolean canDefault) {
		Tokens tokens = getTokens();
		Token firstToken = tokens.next();
		if (!firstToken.isKind(TokenKind.COLON)) {
			tokens.decrementPosition();
			return null;
		}
		StringBuilder destinationName = new StringBuilder();
		Token token = peekDestinationComponentToken();
		if (token == null) {
			if (tokens.peek() == null) {
				tokens.raiseException(firstToken.startPos, DSLMessage.OOD);
			}
			else {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.UNEXPECTED_DATA_IN_DESTINATION_NAME,
						getTokenData(tokens.peek()));
			}
		}
		int startpos = token.startPos;
		destinationName.append(getTokenData(token));
		tokens.next();
		while (tokens.isNextAdjacent() && (token = peekDestinationComponentToken()) != null) {
			destinationName.append(getTokenData(token));
			tokens.next();
		}
		while (tokens.peek(TokenKind.DOT)) {
			if (!tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.NO_WHITESPACE_IN_DESTINATION_DEFINITION);
			}
			tokens.next(); // skip dot
			destinationName.append(TokenKind.DOT.getTokenChars());
			if (!tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.NO_WHITESPACE_IN_DESTINATION_DEFINITION);
			}
			while (tokens.isNextAdjacent() && (token = peekDestinationComponentToken()) != null) {
				destinationName.append(getTokenData(token));
				tokens.next();
			}
		}
		int endPos = token.endPos;
		ArgumentNode[] argumentNodes = eatAppArgs();
		return makeDestinationNode(startpos, endPos, destinationName.toString(), argumentNodes);
	}

	/**
	 * Return a list of {@link AppNode} starting from the current token position.
	 * <p>
	 * Expected formats: {@code appList: app (| app)*} A stream may end in an app (if it is
	 * a sink) or be followed by a sink destination.
	 *
	 * @return a list of {@code AppNode}
	 */
	protected List<AppNode> eatAppList(boolean preceedingSourceChannelSpecified) {
		Tokens tokens = getTokens();
		List<AppNode> appNodes = new ArrayList<AppNode>();
		int usedListDelimiter = -1;
		int usedStreamDelimiter = -1;
		appNodes.add(eatApp());
		while (tokens.hasNext()) {
			Token t = tokens.peek();
			if (t.kind == TokenKind.PIPE) {
				if (usedListDelimiter >= 0) {
					tokens.raiseException(t.startPos, DSLMessage.DONT_MIX_PIPE_AND_DOUBLEPIPE);
				}
				usedStreamDelimiter = t.startPos;
				tokens.next();
				appNodes.add(eatApp());
			}
			else if (t.kind == TokenKind.DOUBLEPIPE) {
				if (preceedingSourceChannelSpecified) {
					tokens.raiseException(t.startPos, DSLMessage.DONT_USE_DOUBLEPIPE_WITH_CHANNELS);
				}
				if (usedStreamDelimiter >= 0) {
					tokens.raiseException(t.startPos, DSLMessage.DONT_MIX_PIPE_AND_DOUBLEPIPE);
				}
				usedListDelimiter = t.startPos;
				tokens.next();
				appNodes.add(eatApp());
			}
			else {
				// might be followed by sink destination
				break;
			}
		}
		boolean isFollowedBySinkChannel = tokens.peek(TokenKind.GT);
		if (isFollowedBySinkChannel && usedListDelimiter >= 0) {
			tokens.raiseException(usedListDelimiter, DSLMessage.DONT_USE_DOUBLEPIPE_WITH_CHANNELS);
		}
		for (AppNode appNode: appNodes) {
			appNode.setUnboundStreamApp(!preceedingSourceChannelSpecified && !isFollowedBySinkChannel && (usedStreamDelimiter < 0));
		}
		return appNodes;
	}

	@Override
	public String toString() {
		Tokens tokens = getTokens();
		return String.valueOf(tokens.getTokenStream()) + "\n" + "tokenStreamPointer=" + tokens.position() + "\n";
	}

	protected DestinationNode makeDestinationNode(int startpos, int endPos, String destinationName, ArgumentNode[] argumentNodes) {
		return new DestinationNode(startpos, endPos, destinationName, argumentNodes);
	}

	protected StreamNode makeStreamNode(String streamText, String streamName, List<AppNode> appNodes,
			SourceDestinationNode sourceDestinationNode, SinkDestinationNode sinkDestinationNode) {
		return new StreamNode(streamText, streamName, appNodes, sourceDestinationNode, sinkDestinationNode);
	}

}
