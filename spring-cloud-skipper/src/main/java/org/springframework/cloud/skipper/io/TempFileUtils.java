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
package org.springframework.cloud.skipper.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mark Pollack
 */
public class TempFileUtils {

	private static final Logger logger = LoggerFactory.getLogger(TempFileUtils.class);

	public static Path createTempDirectory(String rootName) {
		try {
			logger.debug("Creating temp directory with root name {}", rootName);
			Path pathToReturn = Files.createTempDirectory(rootName);
			logger.debug("Created temp directory {}", pathToReturn.toString());
			return pathToReturn;
		}
		catch (IOException e) {
			// todo: This could be SkipperException if SkipperException happens to move into spring-cloud-skipper
			throw new IllegalStateException("Could not create temp directory", e);
		}
	}

}
