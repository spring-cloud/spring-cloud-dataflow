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
package org.springframework.cloud.dataflow.core.dsl;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Oleg Zhurakousky
 * @author Andy Clement
 */
@SpringJUnitConfig
public class NodeTests {

	@Test
	public void testDestinationNodeDestinationName(){
		DestinationNode node = new DestinationNode(0, 0, "foo.bar.bazz", null);
		assertEquals("foo.bar.bazz", node.getDestinationName());
	}

	@Test
	public void testDestinationNodeToString(){
		ArgumentNode an1 = new ArgumentNode("foo", "bar", 0, 4);
		ArgumentNode an2 = new ArgumentNode("abc", "'xyz'", 0, 4);
		DestinationNode node = new DestinationNode(0, 4, "foo.bar.bazz", new ArgumentNode[]{an1, an2});
		System.out.println(node.stringify());
		assertEquals(":foo.bar.bazz", node.toString());
	}

	@Test // see https://github.com/spring-cloud/spring-cloud-dataflow/issues/1568
	public void testStreamNodesToString(){
		ArgumentNode an1 = new ArgumentNode("foo", "bar", 0, 4);
		ArgumentNode an2 = new ArgumentNode("abc", "'xyz'", 0, 4);
		AppNode appNode = new AppNode(null, "bar", 0, 2, new ArgumentNode[]{an1, an2});

		DestinationNode sourceDNode = new DestinationNode(0, 0, "source.bar.bazz", null);
		SourceDestinationNode source = new SourceDestinationNode(sourceDNode, 4);
		DestinationNode sinkDNode = new DestinationNode(0, 0, "sink.bar.bazz", null);
		SinkDestinationNode sink = new SinkDestinationNode(sinkDNode, 4);
		StreamNode sNode = new StreamNode(null, "myStream", Collections.singletonList(appNode), source, sink);
		assertEquals("myStream = :source.bar.bazz > bar --foo=bar --abc='xyz' > :sink.bar.bazz", sNode.toString());
	}
}
