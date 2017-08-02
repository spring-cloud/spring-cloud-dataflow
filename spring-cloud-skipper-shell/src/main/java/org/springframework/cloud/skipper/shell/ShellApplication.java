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

package org.springframework.cloud.skipper.shell;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.skipper.shell.config.ShellConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Bootstrap class for spring shell.
 *
 * @author Ilayaperumal Gopinathan
 * @author Josh Long
 */
@SpringBootApplication
@Import(ShellConfiguration.class)
public class ShellApplication {

	public static void main(String[] args) throws Exception {
		new SpringApplicationBuilder().sources(ShellApplication.class).bannerMode(Banner.Mode.OFF).run(args);
	}
}
