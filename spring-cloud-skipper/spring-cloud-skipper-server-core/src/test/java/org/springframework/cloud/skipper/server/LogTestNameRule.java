/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.server;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mark Pollack
 */
public class LogTestNameRule implements BeforeEachCallback, AfterEachCallback {

	private final static Logger log = LoggerFactory.getLogger("junit.logTestName");

	@Override
	public void afterEach(ExtensionContext extensionContext) throws Exception {
		log.info("Finished Test: {}", extensionContext.getRequiredTestMethod().getName());
	}

	@Override
	public void beforeEach(ExtensionContext extensionContext) throws Exception {
		log.info("Starting Test {}", extensionContext.getRequiredTestMethod().getName());
	}

}
