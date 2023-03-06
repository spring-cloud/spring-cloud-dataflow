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

package org.springframework.cloud.dataflow.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserialize AppBootSchemaVersion with Jackson
 * @author Corneil du Plessis
 */
public class AppBootSchemaVersionDeserializer extends StdDeserializer<AppBootSchemaVersion> {
	public AppBootSchemaVersionDeserializer() {
		super(AppBootSchemaVersion.class);
	}

	public AppBootSchemaVersionDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public AppBootSchemaVersion deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
		String value = jsonParser.getValueAsString();
		return value != null ? AppBootSchemaVersion.fromBootVersion(value) : null;
	}
}
