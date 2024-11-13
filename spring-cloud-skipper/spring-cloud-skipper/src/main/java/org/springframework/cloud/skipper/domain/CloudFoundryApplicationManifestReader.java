/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.cloud.skipper.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import org.springframework.cloud.skipper.SkipperException;

/**
 * Deserializes using Jackson a String to the SpringCloudDeployerApplicationManifest
 * class. Sets {@literal DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES} to
 * {@literal false} so values in the YAML that are not represented in the appKind class
 * will not throw an exception in the deserialization process.
 *
 * @author Ilayaperumal Gopinathan
 * @author Chris Bono
 */
public class CloudFoundryApplicationManifestReader implements SkipperManifestReader {

	private final static Logger logger = LoggerFactory.getLogger(CloudFoundryApplicationManifestReader.class);

	@Override
	public List<CloudFoundryApplicationSkipperManifest> read(String manifest) {
		if (canSupport(manifest)) {
			List<CloudFoundryApplicationSkipperManifest> applicationSpecs = new ArrayList<>();
			YAMLMapper mapper = YAMLMapper.builder().
				configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
				configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).
				propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE).build();
			try {
				MappingIterator<CloudFoundryApplicationSkipperManifest> it = mapper
																	.readerFor(CloudFoundryApplicationSkipperManifest.class)
																	.readValues(manifest);
				while (it.hasNextValue()) {
					CloudFoundryApplicationSkipperManifest appKind = it.next();
					applicationSpecs.add(appKind);
				}
			}
			catch (JsonMappingException e) {
				logger.error("Can't parse Package's manifest YAML = " + manifest);
				throw new SkipperException("JsonMappingException - Can't parse Package's manifest YAML = " + manifest,
						e);
			}
			catch (IOException e) {
				logger.error("Can't parse Package's manifest YAML = " + manifest);
				throw new SkipperException("IOException - Can't parse Package's manifest YAML = " + manifest, e);
			}
			return applicationSpecs;
		}
		return Collections.emptyList();
	}

	public boolean canSupport(String manifest) {
		LoaderOptions options = new LoaderOptions();
		Yaml yaml = new Yaml(new SafeConstructor(options));
		Iterable<Object> object = yaml.loadAll(manifest);
		for (Object o : object) {
			boolean supportKind = assertSupportedKind(o);
			if (!supportKind) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String[] getSupportedKinds() {
		return new String[] {SkipperManifestKind.CloudFoundryApplication.name()};
	}

	private boolean assertSupportedKind(Object object) {
		if (object == null) {
			throw new SkipperException("Can't parse manifest, it is empty");
		}
		Map<String, Object> manifestAsMap;
		if (object instanceof Map) {
			manifestAsMap = (Map<String, Object>) object;
		}
		else {
			throw new SkipperException("Can't parse manifest, it is not a map.  Manifest = " + object);
		}
		Object kindObject = manifestAsMap.get("kind");
		if (kindObject instanceof String kind) {
			if (Arrays.asList(getSupportedKinds()).contains(kind)) {
				logger.debug("Found supported kind " + kind);
				return true;
			}
		}
		return false;
	}
}
