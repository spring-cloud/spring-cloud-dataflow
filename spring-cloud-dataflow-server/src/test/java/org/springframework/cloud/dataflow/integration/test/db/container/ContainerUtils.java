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
package org.springframework.cloud.dataflow.integration.test.db.container;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;

/**
 * Provides for logging the output of test containers.
 *
 * @author Corneil du Plessis
 * @see org.testcontainers.containers.Container#followOutput(Consumer)
 */
public class ContainerUtils {
	private static final Logger logger = LoggerFactory.getLogger(ContainerUtils.class);

	public static void output(String container, OutputFrame outputFrame) {
		switch (outputFrame.getType()) {
			case STDOUT:
				logger.info("{}:{}", container, outputFrame.getUtf8StringWithoutLineEnding());
				break;
			case STDERR:
				logger.error("{}:{}", container, outputFrame.getUtf8StringWithoutLineEnding());
				break;
			case END:
				// don't log logger.info("{}:END:{}", container, outputFrame.getUtf8StringWithoutLineEnding());
				break;

		}
	}

	public static void outputDataFlow(OutputFrame outputFrame) {
		output("dataflow", outputFrame);
	}

	public static void outputSkipper(OutputFrame outputFrame) {
		output("skipper", outputFrame);
	}
}
