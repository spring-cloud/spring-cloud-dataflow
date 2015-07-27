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

package org.springframework.cloud.data.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.yarn.boot.YarnAppmasterAutoConfiguration;
import org.springframework.yarn.boot.YarnClientAutoConfiguration;
import org.springframework.yarn.boot.YarnContainerAutoConfiguration;

/**
 * Bootstrap class for Spring cloud data admin.
 *
 * @author Mark Fisher
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = {
		YarnClientAutoConfiguration.class,
		YarnAppmasterAutoConfiguration.class,
		YarnContainerAutoConfiguration.class,
		OAuth2AutoConfiguration.class
})
public class AdminApplication {

	public static void main(String[] args) {
		List<String> argsList = new ArrayList<>();
		argsList.addAll(Arrays.asList(args));
		argsList.add("--spring.cloud.bootstrap.name=admin");
		SpringApplication.run(AdminApplication.class, argsList.toArray(new String[0]));
	}

}
