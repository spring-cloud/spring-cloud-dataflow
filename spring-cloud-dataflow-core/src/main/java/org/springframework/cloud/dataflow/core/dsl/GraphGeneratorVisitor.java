/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.core.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.springframework.cloud.dataflow.core.dsl.GraphGeneratorVisitor.Context.TransitionTarget;
import org.springframework.cloud.dataflow.core.dsl.graph.Graph;
import org.springframework.cloud.dataflow.core.dsl.graph.Link;
import org.springframework.cloud.dataflow.core.dsl.graph.Node;

/**
 * Visitor that produces a Graph representation of a parsed task definition. This is
 * suprisingly complicated due to the ability to use labels.
 *
 * @author Andy Clement
 */
public class GraphGeneratorVisitor extends TaskVisitor {

	private int nextNodeId = 0;

	// As the visit proceeds different contexts are entered/exited - into a
	// flow, into a split, etc.
	private Stack<GraphGeneratorVisitor.Context> contexts = new Stack<>();

	// The sequences built during the visit
	private List<GraphGeneratorVisitor.Sequence> sequences = new ArrayList<>();

	// Which sequence is currently being visited
	private int currentSequence;

	// When visiting transitions, this is the id of the task app node most
	// recently visited
	private String currentTaskAppId;

	private Map<String, Node> existingNodesToReuse;

	public Graph getGraph() {
		if (sequences.size() == 0) {
			List<Node> nodes = new ArrayList<>();
			List<Link> links = new ArrayList<>();
			nodes.add(new Node("0", "START"));
			nodes.add(new Node("1", "END"));
			links.add(new Link("0", "1"));
			return new Graph(nodes, links);
		}
		else {
			GraphGeneratorVisitor.Sequence s = sequences.get(0);
			Graph g = new Graph(s.nodes, s.links);
			return g;
		}
	}

	private String nextId() {
		return Integer.toString(nextNodeId++);
	}

	@Override
	public boolean preVisitSequence(LabelledTaskNode firstNode, int sequenceNumber) {
		Node sequenceStartNode = new Node(nextId(), "START");
		currentSequence = sequenceNumber;
		sequences.add(new Sequence(sequenceNumber, firstNode.getLabelString(), sequenceStartNode));
		contexts.push(new Context(true, false, sequenceStartNode.id, null));
		return true;
	}

	@Override
	public void postVisitSequence(LabelledTaskNode firstNode, int sequenceNumber) {
		String endId = nextId();
		Node endNode = new Node(endId, "END");
		addLinks(endId);
		addNode(endNode);
		contexts.pop();
	}

	private void addLink(Link link) {
		sequences.get(currentSequence).links.add(link);
	}

	private void addNode(Node node) {
		sequences.get(currentSequence).nodes.add(node);
	}

	private void addLinks(String target) {
		List<String> openNodes = currentContext().getDanglingNodes();
		for (int i = 0; i < openNodes.size(); i++) {
			addLink(new Link(openNodes.get(i), target));
		}
	}

	@Override
	public void endVisit() {
		if (sequences.size() > 0) {
			GraphGeneratorVisitor.Sequence mainSequence = sequences.get(0);
			// iterate until nothing left to do
			int tooMany = 0;
			while (!mainSequence.outstandingTransitions.isEmpty() && tooMany < 50) {
				List<Context.TransitionTarget> nextTransitions = findNextTransitions(
						mainSequence.outstandingTransitions);
				mainSequence.outstandingTransitions.removeAll(nextTransitions);
				GraphGeneratorVisitor.Sequence sequence = findSequence(nextTransitions.get(0).label);
				if (sequence == null) {
					throw new IllegalStateException("Out of flow transition? " + nextTransitions.get(0));
				}
				inline(mainSequence, sequence, nextTransitions);
				// Some transitions might be satisfiable now
				Iterator<TransitionTarget> iter = mainSequence.outstandingTransitions.iterator();
				while (iter.hasNext()) {
					TransitionTarget transitionTarget = iter.next();
					FlowNode flowInWhichTransitionOccurring = transitionTarget.flow;
					Map<String, Node> candidates = mainSequence.labeledNodesInEachFlow
							.get(flowInWhichTransitionOccurring);
					for (Map.Entry<String, Node> candidate : candidates.entrySet()) {
						if (candidate.getKey().equals(transitionTarget.label)) {
							// This is the right one!
							mainSequence.links.add(new Link(transitionTarget.nodeId, candidate.getValue().id,
									transitionTarget.onState));
							iter.remove();
						}
					}
				}
				tooMany++;
			}
		}
	}

	/**
	 * Pick a transition and find if there are any others with the same target. If so
	 * return them all, otherwise just return the picked one.
	 *
	 * @param transitions a list of transitions that might share common targets
	 * @return a single transition (if there are any) and any others with the same target
	 */
	private List<Context.TransitionTarget> findNextTransitions(List<Context.TransitionTarget> transitions) {
		if (transitions.size() == 0) {
			return Collections.emptyList();
		}
		List<Context.TransitionTarget> sameTarget = new ArrayList<>();
		sameTarget.add(transitions.get(0));
		for (int i = 1; i < transitions.size(); i++) {
			Context.TransitionTarget tt = transitions.get(i);
			if (transitions.get(0).flow.equals(tt.flow) && transitions.get(0).label.equals(tt.label)) {
				sameTarget.add(tt);
			}
		}
		return sameTarget;
	}

	private GraphGeneratorVisitor.Sequence findSequence(String label) {
		for (GraphGeneratorVisitor.Sequence sequence : sequences) {
			if (label.equals(sequence.label)) {
				return sequence;
			}
		}
		return null;
	}

	private void inline(GraphGeneratorVisitor.Sequence mainSequence, GraphGeneratorVisitor.Sequence sequence,
			List<Context.TransitionTarget> transitionTargets) {

		// Record a map of ids in the sequence to the new node ids to use when creating
		// new links
		Map<String, String> nodeIds = new HashMap<>();

		// Create copies of all the nodes - except the START and END
		Node startNode = sequence.nodes.get(0);
		Node endNode = sequence.nodes.get(sequence.nodes.size() - 1);
		for (int i = 1; i < (sequence.nodes.size() - 1); i++) {
			Node n = sequence.nodes.get(i);
			Node newNode = new Node(nextId(), n.name, n.properties);
			nodeIds.put(n.id, newNode.id);
			mainSequence.nodes.add(newNode);
		}

		// Now copy links
		for (int i = 0; i < sequence.links.size(); i++) {
			Link existingLink = sequence.links.get(i);
			String existingLinkFrom = existingLink.from;
			String existingLinkTo = existingLink.to;
			Link newLink = null;
			if (existingLinkFrom.equals(startNode.id)) {
				// This link need replacing with links from the transition points to the
				// same
				// target
				for (Context.TransitionTarget tt : transitionTargets) {
					newLink = new Link(tt.nodeId, nodeIds.get(existingLinkTo), tt.onState);
					mainSequence.links.add(newLink);
				}
			}
			else if (existingLinkTo.equals(endNode.id)) {
				// This link needs replacing with links from the final copied node in the
				// sequence
				// to the end node in the main sequence
				Context.TransitionTarget tt = transitionTargets.get(0);
				// assert all the transitionTargets have the same lastNodeId
				String finalNodeInMainSequence = tt.lastNodeId;
				List<Link> newLinks = new ArrayList<>();
				for (Link l : mainSequence.links) {
					if (l.from.equals(finalNodeInMainSequence)) {
						newLinks.add(new Link(nodeIds.get(existingLinkFrom), l.to, l.getTransitionName()));
					}
				}
				mainSequence.links.addAll(newLinks);
			}
			else {
				newLink = new Link(nodeIds.get(existingLinkFrom), nodeIds.get(existingLinkTo),
						existingLink.getTransitionName());
				mainSequence.links.add(newLink);
			}
		}
		// After inlining the sequence, the mainSequence may have inherited new
		// outstanding transitions
		List<Context.TransitionTarget> rewrittenTransitions = new ArrayList<>();
		for (Context.TransitionTarget looseEnd : sequence.outstandingTransitions) {
			Context.TransitionTarget tt = new Context.TransitionTarget(nodeIds.get(looseEnd.nodeId), looseEnd.onState,
					looseEnd.label);
			// They should have the same 'flow' as the sequence they are injected into
			tt.flow = transitionTargets.get(0).flow;
			tt.lastNodeId = transitionTargets.get(0).lastNodeId;// nodeIds.get(looseEnd.lastNodeId);
			rewrittenTransitions.add(tt);
		}
		mainSequence.outstandingTransitions.addAll(rewrittenTransitions);
		// The copy of this secondary sequence is being inserted into a particular flow.
		FlowNode flowBeingInsertedInto = transitionTargets.get(0).flow;
		Map<String, Node> relevantFlowMapToUpdate = mainSequence.labeledNodesInEachFlow.get(flowBeingInsertedInto);
		Map<FlowNode, Map<String, Node>> labeledNodesInSequenceBeingInlined = sequence.labeledNodesInEachFlow;
		FlowNode primaryFlowInSequenceBeingInlined = sequence.primaryFlow;
		for (Map.Entry<FlowNode, Map<String, Node>> entry : labeledNodesInSequenceBeingInlined.entrySet()) {
			if (entry.getKey() == primaryFlowInSequenceBeingInlined) {
				// these should be remapped
				for (Map.Entry<String, Node> entry2 : entry.getValue().entrySet()) {
					Node n2 = new Node(nodeIds.get(entry2.getKey()), entry2.getValue().name);
					n2.setLabel(entry2.getValue().getLabel());
					relevantFlowMapToUpdate.put(entry2.getKey(), n2);
				}
			}
			else {
				// these can just be copied direct
				Map<String, Node> newMap = new HashMap<>();
				for (Map.Entry<String, Node> entry2 : entry.getValue().entrySet()) {
					Node n2 = new Node(nodeIds.get(entry2.getKey()), entry2.getValue().name);
					n2.setLabel(entry2.getValue().getLabel());
					newMap.put(entry2.getKey(), n2);
				}
				mainSequence.labeledNodesInEachFlow.put(entry.getKey(), newMap);
			}
		}

	}

	@Override
	public boolean preVisit(SplitNode split) {
		List<String> open = currentContext().getDanglingNodes();
		String startId = (open.size() == 0 ? currentContext().startNodeId : open.get(0));
		// If there are multiple open nodes, we need a sync node !
		if (open.size() > 1) {
			String syncId = nextId();
			Node node = new Node(syncId, "SYNC");
			addNode(node);
			for (String openid : open) {
				addLink(new Link(openid, syncId));
			}
			startId = syncId;
		}
		contexts.push(new Context(false, true, startId, split));
		return true;
	}

	@Override
	public void postVisit(SplitNode split) {
		List<String> openAtEndOfFlow = currentContext().getDanglingNodes();
		contexts.pop();
		currentContext().addDanglingNodes(true, openAtEndOfFlow);
	}

	@Override
	public boolean preVisit(FlowNode flow) {
		contexts.push(new Context(true, false, currentContext().startNodeId, flow));
		currentSequence().primaryFlow = flow;
		return true;
	}

	@Override
	public void postVisit(FlowNode flow) {
		// What label references were not resolved within the flow?
		List<Context.TransitionTarget> transitionTargets = currentContext().getTransitionTargets();
		// For all outstanding transitions, mark them indicating which flow they came from
		// and the last node of that flow. Thus when they are processed later, after the
		// transition completes it knows where to join the output to.
		for (Context.TransitionTarget tt : transitionTargets) {
			tt.lastNodeId = currentContext().getDanglingNodes().get(0);
			tt.flow = flow;
		}
		sequences.get(currentSequence).outstandingTransitions.addAll(transitionTargets);
		currentSequence().labeledNodesInEachFlow.put(flow, currentContext().nodesWithLabels);
		List<String> openAtEndOfFlow = currentContext().getDanglingNodes();
		List<String> otherExitNodes = currentContext().otherExits;
		contexts.pop();
		currentContext().addDanglingNodes(false, openAtEndOfFlow);
		currentContext().addDanglingNodes(false, otherExitNodes);
	}

	private Node findOrMakeNode(String name) {
		Node node = existingNodesToReuse.get(name);
		if (node == null) {
			node = new Node(nextId(), name);
			existingNodesToReuse.put(name, node);
			addNode(node);
		}
		return node;
	}

	@Override
	public void visit(TransitionNode transition) {
		if (transition.isTargetApp()) {
			if (transition.isSpecialTransition()) {
				if (transition.isFailTransition()) {
					Node failNode = findOrMakeNode("$FAIL");
					addLink(new Link(currentTaskAppId, failNode.id, transition.getStatusToCheck()));
				}
				else if (transition.isEndTransition()) {
					Node endNode = findOrMakeNode("$END");
					addLink(new Link(currentTaskAppId, endNode.id, transition.getStatusToCheck()));
				}
			}
			else {
				String key = toKey(transition.getTargetApp());
				Node n = existingNodesToReuse.get(key);
				boolean isCreated = false;
				if (n == null) {
					String nextId = nextId();
					n = new Node(nextId, transition.getTargetApp().getName(),
							toMap(transition.getTargetApp().getArguments()));
					if (transition.getTargetApp().hasLabel()) {
						n.setLabel(transition.getTargetApp().getLabelString());
					}
					existingNodesToReuse.put(key, n);
					addNode(n);
					isCreated = true;
				}
				addLink(new Link(currentTaskAppId, n.id, transition.getStatusToCheck()));
				if (isCreated) {
					if (currentContext().isFlow) {
						currentContext().addOtherExit(n.id);
					}
					else {
						currentContext().addDanglingNodes(false, n.id);
					}
				}
			}
		}
		else {
			// Check if it is a transition to something labeled earlier in this flow
			Node existingNode = currentContext().getNodeLabeled(transition.getTargetLabel());
			if (existingNode != null) {
				addLink(new Link(currentTaskAppId, existingNode.id, transition.getStatusToCheck()));
			}
			else {
				// Record a new transition attempted
				currentContext().addTransitionTarget(currentTaskAppId, transition.getStatusToCheck(),
						transition.getTargetLabel());
			}
		}
	}

	private Map<String, String> toMap(ArgumentNode[] arguments) {
		if (arguments == null) {
			return null;
		}
		Map<String, String> argumentsMap = new HashMap<>();
		for (ArgumentNode argument : arguments) {
			argumentsMap.put(argument.getName(), argument.getValue());
		}
		return argumentsMap;
	}

	/**
	 * Create a unique map key for a given target app and options.
	 */
	private String toKey(TaskAppNode targetApp) {
		StringBuilder key = new StringBuilder();
		if (targetApp.hasLabel()) {
			key.append(targetApp.getLabel()).append(">");
		}
		key.append(targetApp.getName());
		for (Map.Entry<String, String> argument : targetApp.getArgumentsAsMap().entrySet()) {
			key.append(":").append(argument.getKey()).append("=").append(argument.getValue());
		}
		return key.toString();
	}

	public GraphGeneratorVisitor.Context currentContext() {
		return contexts.peek();
	}

	public GraphGeneratorVisitor.Context parentContext() {
		if (contexts.size() < 2) {
			return null;
		}
		else {
			return contexts.get(contexts.size() - 2);
		}
	}

	public Sequence currentSequence() {
		return sequences.get(currentSequence);
	}

	@Override
	public void visit(TaskAppNode taskApp) {
		String nextId = nextId();
		currentTaskAppId = nextId;
		Node node = new Node(nextId, taskApp.getName(), toMap(taskApp.getArguments()));
		addNode(node);
		if (taskApp.hasLabel()) {
			node.setLabel(taskApp.getLabelString());
			currentContext().addNodeWithLabel(taskApp.getLabelString(), node);
		}
		if (currentContext().isFlow) {
			// Are there any outstanding transitions that need to be connected
			// to this?
			if (taskApp.hasLabel()) {
				// If this one has a label, try to connect hanging transitions to it
				for (Iterator<Context.TransitionTarget> iterator = currentContext().getTransitionTargets()
						.iterator(); iterator.hasNext();) {
					Context.TransitionTarget tt = iterator.next();
					if (tt.label.equals(taskApp.getLabelString())) {
						// Target found!
						addLink(new Link(tt.nodeId, nextId, tt.onState));
						iterator.remove();
					}
				}
			}
			List<String> danglingNodes = currentContext().getDanglingNodes();
			if (danglingNodes.size() == 0) {
				// This app is the first in the flow and will create the first dangling
				// node
				addLink(new Link(currentContext().startNodeId, nextId));
			}
			else {
				// Everything outstanding needs joining to this node
				for (int i = 0; i < danglingNodes.size(); i++) {
					addLink(new Link(danglingNodes.get(i), nextId));
				}
			}
			currentContext().addDanglingNodes(true, nextId);
		}
		else if (currentContext().isSplit) {
			// Any flow containing this split can transition to this split if it
			// is labeled. This code
			// isn't complete as it will only link it to the first one in the
			// split, when it needs to
			// link to all of them. Or we create a sync node and link to that.
			if (currentContext().containingNode.hasLabel() && (parentContext() != null && parentContext().isFlow)) {
				// A surrounding flow can target a split with a transition
				// target
			}

			// If visiting a taskapp in a split, it means there is no
			// surrounding flow
			addLink(new Link(currentContext().startNodeId, nextId));
			currentContext().addDanglingNodes(false, nextId);
		}
		existingNodesToReuse = (currentContext().isFlow ? currentContext().extraNodes : new HashMap<>());
	}

	// Gathers knowledge about a sequence during visiting
	static class Sequence {

		// Which sequence number is this
		final int sequenceNumber;

		// Name of this sequence (all but the first must have a label)
		final String label;

		// Nodes in this sequence
		final List<Node> nodes = new ArrayList<>();

		// Links in this sequence
		final List<Link> links = new ArrayList<>();

		// Transitions made from inside this sequence which are not satisfied
		final List<Context.TransitionTarget> outstandingTransitions = new ArrayList<>();

		final Map<FlowNode, Map<String, Node>> labeledNodesInEachFlow = new HashMap<>();

		FlowNode primaryFlow;

		Sequence(int sequenceNumber, String label, Node sequenceStartNode) {
			this.sequenceNumber = sequenceNumber;
			this.label = label;
			nodes.add(sequenceStartNode);
		}

		public String toString() {
			StringBuilder s = new StringBuilder();
			for (Node n : nodes) {
				s.append("[").append(n.id).append(":").append(n.name).append("]");
			}
			for (Link l : links) {
				s.append("[" + (l.getTransitionName() == null ? "" : l.getTransitionName() + ":") + l.from + "-" + l.to
						+ "]");
			}
			s.append("  transitions:").append(outstandingTransitions);
			s.append("   flowLabelsMap:").append(labeledNodesInEachFlow);
			return s.toString();
		}

	}

	static class Context {

		// Nodes in this sequence that are labeled are recorded here
		final Map<String, Node> nodesWithLabels = new HashMap<>();

		// When a transition branch is taken to some other app within a flow,
		// this records the app node that must be joined to the exit path
		// from the entire flow.
		public List<String> otherExits = new ArrayList<>();

		// For forward references this keeps track of them so that they can be
		// filled in later. The reference must be satisfied within the same flow
		// or by a separate sequence. If that secondary sequence routes back
		// to the primary, it must be within the same flow!
		public List<Context.TransitionTarget> transitionTargets = new ArrayList<>();

		public Map<String, Node> extraNodes = new HashMap<>();

		// Within a flow, transitions to the 'same job' would share a node
		// target, as would all transitions to an $END or $FAIL node. This
		// keeps track of what has been created so it can be reused.
		Map<String, Node> nodesSharedInFlow = new LinkedHashMap<String, Node>();

		// When processing apps in a real Flow or Split, this is the Ast node
		// for that Flow or Split.
		LabelledTaskNode containingNode;

		// Tracking what kind of context we are in
		boolean isFlow = false;

		boolean isSplit = false;

		// Id of the first node in this context (start of a flow, start of a
		// split)
		private String startNodeId;

		// As a flow/split is processed gradually we accumulate dangling nodes,
		// those that need connecting to whatever comes next. For a flow there might
		// just be one, but for a split multiple.
		private List<String> currentDanglingNodes = new ArrayList<>();

		Context(boolean isFlow, boolean isSplit, String startNodeId, LabelledTaskNode split) {
			this.isFlow = isFlow;
			this.isSplit = isSplit;
			this.startNodeId = startNodeId;
			this.containingNode = split;
		}

		public void addDanglingNodes(boolean replaceExisting, String... is) {
			if (replaceExisting) {
				currentDanglingNodes.clear();
			}
			for (String i : is) {
				currentDanglingNodes.add(i);
			}
		}

		public void addDanglingNodes(boolean replaceExisting, List<String> newDanglingNodes) {
			if (replaceExisting) {
				currentDanglingNodes.clear();
			}
			currentDanglingNodes.addAll(newDanglingNodes);
		}

		public List<String> getDanglingNodes() {
			return currentDanglingNodes;
		}

		public void clearDanglingNodes() {
			currentDanglingNodes.clear();
		}

		public void addTransitionTarget(String fromNodeId, String fromOnState, String targetLabel) {
			Context.TransitionTarget tt = new TransitionTarget(fromNodeId, fromOnState, targetLabel);
			transitionTargets.add(tt);
		}

		public List<TransitionTarget> getTransitionTargets() {
			return this.transitionTargets;
		}

		public void addOtherExit(String nextId) {
			otherExits.add(nextId);
		}

		public Node getNodeLabeled(String label) {
			return nodesWithLabels.get(label);
		}

		public void addNodeWithLabel(String label, Node node) {
			nodesWithLabels.put(label, node);
		}

		static class TransitionTarget {
			// The node to transition from
			String nodeId;

			// The state to be checked that would cause this transition
			String onState;

			// The label to be connected to
			String label;

			// Last node in flow, when joining things up, anywhere this got
			// joined to, the inserted stuff will need to be joined to
			String lastNodeId;

			// Which flow was this transition in
			FlowNode flow;

			TransitionTarget(String fromNodeId, String fromOnState, String targetLabel) {
				this.nodeId = fromNodeId;
				this.onState = fromOnState;
				this.label = targetLabel;
			}

			public String toString() {
				StringBuilder s = new StringBuilder();
				s.append(nodeId).append(":").append(onState).append("->").append(label);
				return s.toString();
			}
		}

	}

}
