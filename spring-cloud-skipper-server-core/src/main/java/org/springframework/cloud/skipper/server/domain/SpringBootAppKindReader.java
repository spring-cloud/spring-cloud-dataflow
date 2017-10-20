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
package org.springframework.cloud.skipper.server.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.SkipperException;

/**
 * Deserializes using Jackson a String to a {@link SpringBootAppKind} class. Sets
 * {@literal DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES} to {@literal false} so
 * values in the YAML that are not represented in the SpringBootAppKind class will not
 * throw an exception in the deserialization process.
 * @author Mark Pollack
 */
public abstract class SpringBootAppKindReader {

	private final static Logger logger = LoggerFactory.getLogger(SpringBootAppKindReader.class);

	public static List<SpringBootAppKind> read(String manifest) {
		List<SpringBootAppKind> springBootAppKindList = new ArrayList<>();
		YAMLMapper mapper = new YAMLMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			MappingIterator<SpringBootAppKind> it = mapper.readerFor(SpringBootAppKind.class).readValues(manifest);
			while (it.hasNextValue()) {
				SpringBootAppKind springBootAppKind = it.next();
				springBootAppKindList.add(springBootAppKind);
			}
		}
		catch (JsonMappingException e) {
			logger.error("Can't parse Package's manifest YAML = " + manifest);
			throw new SkipperException("JsonMappingException - Can't parse Package's manifest YAML = " + manifest, e);
		}
		catch (IOException e) {
			logger.error("Can't parse Package's manifest YAML = " + manifest);
			throw new SkipperException("IOException - Can't parse Package's manifest YAML = " + manifest, e);
		}
		return springBootAppKindList;
	}
}
