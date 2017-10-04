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
package org.springframework.cloud.skipper.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * @author Mark Pollack
 */
public abstract class SpringBootAppKindReader {

	public static List<SpringBootAppKind> read(String manifest) {
		List<SpringBootAppKind> springBootAppKindList = new ArrayList<>();
		YAMLMapper mapper = new YAMLMapper();
		try {
			MappingIterator<SpringBootAppKind> it = mapper.readerFor(SpringBootAppKind.class).readValues(manifest);
			while (it.hasNextValue()) {
				SpringBootAppKind springBootAppKind = it.next();
				springBootAppKindList.add(springBootAppKind);
			}
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't parse Package's manifest YAML", e);
		}
		return springBootAppKindList;
	}
}
