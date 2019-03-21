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
package org.springframework.cloud.skipper.shell.command;

import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.shell.command.support.SkipperClientUpdatedEvent;
import org.springframework.context.event.EventListener;

/**
 * Contains a common reference for {@link SkipperClient} and an {@link EventListener} to
 * get updates made through the 'skipper config' command.
 *
 * @author Mark Pollack
 */
public abstract class AbstractSkipperCommand {

	// Updated via event publication
	protected SkipperClient skipperClient;

	@EventListener
	void handle(SkipperClientUpdatedEvent event) {
		this.skipperClient = event.getSkipperClient();
	}
}
