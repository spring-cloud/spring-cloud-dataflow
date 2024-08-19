/*
 * Copyright 2016 the original author or authors.
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
import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobParameter;

/**
 * Jackson Deserializer for {@link JobParameter} de-serialization.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public class JobParameterJacksonDeserializer extends JsonDeserializer<JobParameter> {

	private final Logger logger = LoggerFactory.getLogger(JobParameterJacksonDeserializer.class);

	@Override
	public JobParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
			throws IOException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);

		String value = node.get("value").asText();
		boolean identifying = node.get("identifying").asBoolean();
		String type = node.get("type").asText();

		JobParameter jobParameter;
		if (!type.isEmpty()) {
			try {
				jobParameter = new JobParameter(value, Class.forName(type), identifying);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("JobParameter type %s is not supported by DataFlow".formatted(type), e);
			}
		}
		else {
            jobParameter = new JobParameter(value, String.class, identifying);
        }

		if (logger.isDebugEnabled()) {
			logger.debug("jobParameter - value: {} (type: {}, isIdentifying: {})",
					jobParameter.getValue(), jobParameter.getType(), jobParameter.isIdentifying());
		}

		return jobParameter;
	}
}
