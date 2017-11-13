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
package org.springframework.cloud.skipper.shell.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.skipper.client.SkipperClientProperties;
import org.springframework.cloud.skipper.shell.command.support.Target;
import org.springframework.cloud.skipper.shell.command.support.TargetHolder;
import org.springframework.core.annotation.Order;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.jline.DefaultShellApplicationRunner;

/**
 * {@link ApplicationRunner} that attempts the initial targetting of the Skipper server.
 *
 * @author Josh Long
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 *
 * @see DefaultShellApplicationRunner
 * @see HelpAwareShellApplicationRunner
 */
@Order(DefaultShellApplicationRunner.PRECEDENCE - 10)
public class InitialConnectionApplicationRunner implements ApplicationRunner {

	@Autowired
	private TargetHolder targetHolder;

	@Autowired
	private SkipperClientProperties clientProperties;

	@Autowired
	@Qualifier("main")
	private ResultHandler<Exception> resultHandler;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Target target = new Target(clientProperties.getUri(), clientProperties.getUsername(),
				clientProperties.getPassword(), clientProperties.isSkipSslValidation());

		// Attempt connection (including against default values) but do not crash the shell on
		// error
		try {
			targetHolder.changeTarget(target, null);
		}
		catch (Exception e) {
			resultHandler.handleResult(e);
		}
	}
}
