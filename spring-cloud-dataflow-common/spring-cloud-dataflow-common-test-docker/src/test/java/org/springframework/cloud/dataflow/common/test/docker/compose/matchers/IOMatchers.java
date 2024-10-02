/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose.matchers;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Condition;

public final class IOMatchers {
	private IOMatchers() {}
	public static Condition<File[]> containsInAnyOrder(Condition<File>... conditions) {
		return new Condition<>(files ->
			Arrays.stream(conditions).allMatch(condition -> Arrays.stream(files).anyMatch(condition::matches))
		, "containsInAnyOrder");
	}
	public static Condition<File> hasFiles(int numberOfFiles) {
		return new Condition<File>(dir -> dir.isDirectory() && dir.listFiles().length == numberOfFiles, "directory has " + numberOfFiles + " of files");
	}

	public static Condition<File> fileWithName(String filename) {
		return new Condition<>(file -> file.getName().equals(filename), "filename is '" + filename + "'");
	}

	public static Condition<File> fileContainingString(String contents) {
		return fileWithContents(new Condition<>(s -> s.contains(contents), "contains " + contents));
	}

	public static Condition<File> fileWithContents(Condition<String> contentsMatcher) {
		return new Condition<>(file -> {
            try {
                return contentsMatcher.matches(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "file contents");
	}

}
