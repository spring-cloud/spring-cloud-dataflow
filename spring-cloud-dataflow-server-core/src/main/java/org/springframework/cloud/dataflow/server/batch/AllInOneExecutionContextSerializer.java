/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.batch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;

/**
 * Implements the same logic as used in Batch 5.x
 * @author Corneil du Plessis
 */
public class AllInOneExecutionContextSerializer extends Jackson2ExecutionContextStringSerializer {
	private final static Logger logger = LoggerFactory.getLogger(AllInOneExecutionContextSerializer.class);
	@SuppressWarnings({"unchecked", "NullableProblems"})
	@Override
	public Map<String, Object> deserialize(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		IOUtils.copy(inputStream, buffer);
		Map<String, Object> result = new HashMap<>();
		// Try Jackson
		try {
			return super.deserialize(new ByteArrayInputStream(buffer.toByteArray()));
		} catch (Throwable x) {
			result.put("context.deserialize.error.jackson", x.toString());
		}
		InputStream decodingStream = new ByteArrayInputStream(buffer.toByteArray());
		try {
			// Try decode base64
			decodingStream = Base64.getDecoder().wrap(decodingStream);
		} catch (Throwable x) {
			// Use original input for java deserialization
			decodingStream = new ByteArrayInputStream(buffer.toByteArray());
			result.put("context.deserialize.error.base64.decode", x.toString());
		}
		try {
			ObjectInputStream objectInputStream = new ObjectInputStream(decodingStream);
			return (Map<String, Object>) objectInputStream.readObject();
		} catch (Throwable x) {
			result.put("context.deserialize.error.java.deserialization", x.toString());
		}
		// They may have a custom serializer or custom classes.
		logger.warn("deserialization failed:{}", result);
		return result;
    }
}
