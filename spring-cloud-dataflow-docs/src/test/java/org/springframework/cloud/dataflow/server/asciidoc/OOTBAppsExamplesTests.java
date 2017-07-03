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

package org.springframework.cloud.dataflow.server.asciidoc;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Checks that for each app mentioned in {@literal ootb-apps.adoc}, there is a corresponding example file present.
 *
 * <p>When run as a {@literal main()}, creates stub files.</p>
 */
public class OOTBAppsExamplesTests {

	private static final Pattern REMOTE_INCLUDE =
		Pattern.compile("\\Qinclude::{stream-app-starters-root}/\\E(?:[^/]+)\\Q/{scsa-branch}/spring-cloud-starter-stream-\\E(?<type>source|processor|sink)-(?<name>[^/]+)\\Q/README.adoc[tags=ref-doc]\\E");

	public static final String MAIN_FILE = "src/main/asciidoc/ootb-apps.adoc";

	public static final String EXPECTED_FILE = "src/main/asciidoc/ootb-apps.adoc.expected";

	private LineNumberReader reader;

	private List<String> expected = new ArrayList<>();

	private List<String> errors = new ArrayList<>();

	private String lastReadLine;


	@Test
	public void checkExampleIncludes() throws Exception {
		reader = new LineNumberReader(new FileReader(MAIN_FILE));
		List<String> lookAhead = new ArrayList<>();
		String type = null;
		String name = null;

		while ((lastReadLine = reader.readLine()) != null) {
			Matcher matcher = REMOTE_INCLUDE.matcher(lastReadLine);
			if (matcher.matches()) {
				type = matcher.group("type");
				name = matcher.group("name");
				expected.add(lastReadLine);
				lastReadLine = null;
				// Expect newline
				readLineExpecting("");
				// Expect include of SCDF specific file
				String example = String.format("include::ootb-apps/%s-%s.adoc[]", type, name);
				File file = new File(String.format("src/main/asciidoc/ootb-apps/%s-%s.adoc", type, name));
				readLineExpecting(example);
				if (!file.exists()) {
					errors.add(file + " did not exist. Stub file has been created");
					try (FileWriter content = new FileWriter(file)) {
						content.append("//== DSL Example\n\n//TODO\n");
					}
				}
				readLineExpecting("");
			} else {
				type = name = null;
			}
			if (lastReadLine != null) {
				expected.add(lastReadLine);
			}
		}
		if (!errors.isEmpty()) {
			try (FileWriter writer = new FileWriter(EXPECTED_FILE)) {
				expected.forEach(l -> {
					try {
						writer.append(l).append('\n');
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
			fail("The following errors were encountered.\n" +
				"The expected contents of " + MAIN_FILE + " has been written as " + EXPECTED_FILE + " for comparison.\n"
			+ errors.stream().collect(Collectors.joining("\n")));
		}
	}

	private void readLineExpecting(String what) throws IOException {
		if (lastReadLine == null) {
			lastReadLine = reader.readLine();
		}
		if (!what.equals(lastReadLine)) {
			errors.add("Unexpected content at line " + reader.getLineNumber());
		} else {
			lastReadLine = null;
		}
		expected.add(what);
	}

}
