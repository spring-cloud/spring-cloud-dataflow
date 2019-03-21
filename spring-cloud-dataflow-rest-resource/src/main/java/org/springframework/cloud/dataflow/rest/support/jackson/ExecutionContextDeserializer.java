/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.support.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.batch.item.ExecutionContext;

/**
 * Jackson Deserializer for {@link ExecutionContext} de-serialization. The deserializer
 * applies the following rule:
 *
 * It first tests for numeric values:
 *
 * <ol>
 *   <li>If value is an integral number and fits into an int, then add it as an int
 *   <li>If value does not fit into an int add it as a long
 *   <li>If value is a floating point number, then add it as a double
 *   <li>If value is a boolean, then add it as a string
 *   <li>If value is a text, then add it as a string
 *   <li>Else {@link JsonNode#toString()} is add as a String (Including for Arrays)
 * </ol>
 *
 * @author Gunnar Hillert
 * @since 2.0
 */
public class ExecutionContextDeserializer extends JsonDeserializer<ExecutionContext> {

	@Override
	public ExecutionContext deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
			throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);

		final ExecutionContext executionContext = new ExecutionContext();
		final boolean dirty = node.get("dirty").asBoolean();

		for (JsonNode valueNode : node.get("values")) {

			final JsonNode nodeValue = valueNode.fields().next().getValue();
			final String nodeKey = valueNode.fields().next().getKey();

			if (nodeValue.isNumber() && !nodeValue.isFloatingPointNumber() && nodeValue.canConvertToInt()) {
				executionContext.putInt(nodeKey, nodeValue.asInt());
			}
			else if (nodeValue.isNumber() && !nodeValue.isFloatingPointNumber() && nodeValue.canConvertToLong()) {
				executionContext.putLong(nodeKey, nodeValue.asLong());
			}
			else if (nodeValue.isFloatingPointNumber()) {
				executionContext.putDouble(nodeKey, nodeValue.asDouble());
			}
			else if (nodeValue.isBoolean()) {
				executionContext.putString(nodeKey, String.valueOf(nodeValue.asBoolean()));
			}
			else if (nodeValue.isTextual()) {
				executionContext.putString(nodeKey, nodeValue.asText());
			}
			else {
				executionContext.put(nodeKey, nodeValue.toString());
			}
		}

		if (!dirty && executionContext.isDirty()) {
			executionContext.clearDirtyFlag();
		}

		return executionContext;
	}
}
