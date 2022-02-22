/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.config;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * A provider that sets the shell prompt to 'dataflow' if the server is available, 'server-unknown' otherwise.
 *
 * @author Ilayaperumal Gopinathan
 * @author Chris Bono
 */
@Component
public class DataFlowPromptProvider implements PromptProvider {

	@Autowired
	private DataFlowShell shell;

	@Override
	public AttributedString getPrompt() {
		if (shell.getDataFlowOperations() != null) {
			return new AttributedString("dataflow:>", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
		}
		else {
			return new AttributedString("server-unknown:>", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
		}
	}
}
