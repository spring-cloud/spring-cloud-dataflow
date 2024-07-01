/*
 * Copyright 2024 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class JobParameterJacksonDeserializerTests {

	@Test
	public void validJobParameter() throws IOException {
		JobParameterJacksonDeserializer jobParameterJacksonDeserializer = new JobParameterJacksonDeserializer();
		String json = "{\"value\":\"BAR\",\"type\":\"java.lang.String\",\"identifying\":true}";
		JobParameter jobParameter = jobParameterJacksonDeserializer.deserialize(getJsonParser(json), null);
		assertThat(jobParameter.getType()).isEqualTo(String.class);
		assertThat(jobParameter.getValue()).isEqualTo("BAR");
		assertThat(jobParameter.isIdentifying()).isTrue();
	}

	@Test
	public void inValidJobParameter() throws IOException {
		JobParameterJacksonDeserializer jobParameterJacksonDeserializer = new JobParameterJacksonDeserializer();
		String json = "{\"value\":\"BAR\",\"type\":\"java.lang.FOO\",\"identifying\":true}";
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> {
				jobParameterJacksonDeserializer.deserialize(getJsonParser(json), null);
			})
			.withMessage("JobParameter type java.lang.FOO is not supported by DataFlow");
	}

	private JsonParser getJsonParser(String json) throws IOException {
		JsonFactory factory = new JsonFactory();
		byte[] jsonData = json.getBytes();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonData);
		UTF8StreamJsonParser jsonParser = (UTF8StreamJsonParser) factory.createParser(inputStream);
		jsonParser.setCodec(new ObjectMapper());
		return jsonParser;
	}
}
