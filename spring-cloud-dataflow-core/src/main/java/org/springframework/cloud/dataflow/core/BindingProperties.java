/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.cloud.dataflow.core;

/**
 * Spring Cloud Stream property names mostly used for binding.
 *
 * @author Ilayaperumal Gopinathan
 */
public class BindingProperties {

	public static final String COUNT_PROPERTY = "count";

	public static final String PARTITIONED_PROPERTY = "partitioned";

	public static final String PARTITION_KEY_EXPRESSION = "partitionKeyExpression";

	public static final String PARTITION_KEY_EXTRACTOR_CLASS = "partitionKeyExtractorClass";

	public static final String PARTITION_SELECTOR_CLASS = "partitionSelectorClass";

	public static final String PARTITION_SELECTOR_EXPRESSION = "partitionSelectorExpression";

	public static final String ROOT_PREFIX = "spring.cloud.stream.";

	public static final String INSTANCE_COUNT = ROOT_PREFIX + "instanceCount";

	public static final String BINDING_KEY_PREFIX = ROOT_PREFIX + "bindings.";

	public static final String INPUT_BINDING_KEY_PREFIX = BINDING_KEY_PREFIX + "input.";

	public static final String OUTPUT_BINDING_KEY_PREFIX = BINDING_KEY_PREFIX + "output.";

	public static final String CONTENT_TYPE_KEY = "contentType";

	/**
	 * Default property key for input channel binding.
	 */
	public static final String INPUT_BINDING_KEY = INPUT_BINDING_KEY_PREFIX + "destination";

	public static final String INPUT_PARTITIONED = INPUT_BINDING_KEY_PREFIX + PARTITIONED_PROPERTY;

	/**
	 * Default property key for output channel binding.
	 */
	public static final String OUTPUT_BINDING_KEY = OUTPUT_BINDING_KEY_PREFIX + "destination";

	public static final String OUTPUT_PARTITION_KEY_EXPRESSION = OUTPUT_BINDING_KEY_PREFIX + PARTITION_KEY_EXPRESSION;

	public static final String OUTPUT_PARTITION_KEY_EXTRACTOR_CLASS = OUTPUT_BINDING_KEY_PREFIX + PARTITION_KEY_EXTRACTOR_CLASS;

	public static final String OUTPUT_PARTITION_SELECTOR_CLASS = OUTPUT_BINDING_KEY_PREFIX + PARTITION_SELECTOR_CLASS;

	public static final String OUTPUT_PARTITION_SELECTOR_EXPRESSION = OUTPUT_BINDING_KEY_PREFIX + PARTITION_SELECTOR_EXPRESSION;

	public static final String OUTPUT_PARTITION_COUNT = OUTPUT_BINDING_KEY_PREFIX + "partitionCount";

	/**
	 * Type conversion for channel binding
	 */
	public static final String INPUT_TYPE_KEY = INPUT_BINDING_KEY_PREFIX + CONTENT_TYPE_KEY;

	public static final String OUTPUT_TYPE_KEY = OUTPUT_BINDING_KEY_PREFIX + CONTENT_TYPE_KEY;

}
